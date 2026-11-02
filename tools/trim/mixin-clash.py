#!/usr/bin/env python3
"""Vanilla methods that Nytheria Compat's mixins and other pack mods' mixins both hook. Dev tool, not shipped.

    py -3 tools/trim/mixin-clash.py [--mods DIR] [--out FILE]      (needs ./gradlew compileJava first)

Reads every mixin config of every Fabric jar in the pack (nested jars included, Fabric API modules too) and Nytheria Compat's compiled
mixins (build/classes), reads each handler's target class, target method(s), injector kind and @At targets, converts the pack's
intermediary names to Mojang names with src/main/resources/moj_intermediary.tiny and joins on class#method+descriptor.
The four NeoForge mods are not "other mods". Jars nested in the Nytheria Compat jar (Porting Lib and so on) are listed as `bundled`
and never count as another mod. Accessors and invokers are not hooks and are skipped.

Severity: high   either side has @Overwrite or @Redirect, or both sides change the same call or return value
          medium a handler on either side can cancel or skip (Inject that calls cancel/setReturnValue, WrapWithCondition,
                 Redirect, WrapOperation/WrapMethod that never calls the original)
          A WrapOperation/WrapMethod that calls the original may still skip it on some paths; classfile.py keeps no branch
          information, so those show as `conditional:` in nytheria_compat_can_cancel_or_skip and event_skippable and do not raise severity.
          low    everything else
Limits: @At ordinal/shift and other int/boolean elements are not read (classfile.py drops them), so `same_at` can over-report
when two handlers use the same target at different ordinals. Mixin config plugins that drop mixins at runtime are not run.
Fabric events are found by reading the handler's bytecode (three call levels deep into the mixin class and net/fabricmc/**)
for reads of an `Event` field of net/fabricmc/fabric/api/**, only in handlers of jars whose mod id starts with `fabric`.
"""
import argparse
import collections
import importlib.util
import io
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("classfile", Path(__file__).with_name("classfile.py"))
classfile = importlib.util.module_from_spec(spec)
sys.modules["classfile"] = classfile
spec.loader.exec_module(classfile)

TINY = ROOT / "src/main/resources/moj_intermediary.tiny"
MODS = Path.home() / "curseforge/minecraft/Instances/NytheriaDevelopment/mods"
OUT = Path(__file__).with_name("mixin-clash.tsv")
NYTHERIACOMPAT_CONFIGS = ["src/main/resources/nytheria_compat.mixins.json", "src/main/resources/nytheria_compat_neo_injects.mixins.json",
                   "src/main/resources/nytheria_compat_neo_modifications.mixins.json", "forge/src/main/resources/neoforge.mixins.json",
                   "loader/fabric/src/main/resources/knit_loader.fabric.mixins.json",
                   "compat/fabric-compats/src/main/resources/nytheria_compat_fabric_compats.mixins.json",
                   "compat/forge-config-api/src/main/resources/nytheria_compat_neo_config_compat.mixins.json"]
CLASS_ROOTS = ["build/classes/java/main", "forge/build/classes/java/main", "loader/fabric/build/classes/java/main",
               "compat/fabric-compats/build/classes/java/main", "compat/forge-config-api/build/classes/java/main"]

KINDS = {"Inject", "Redirect", "Overwrite", "ModifyArg", "ModifyArgs", "ModifyVariable", "ModifyConstant", "WrapOperation",
         "WrapWithCondition", "WrapMethod", "ModifyExpressionValue", "ModifyReceiver", "ModifyReturnValue"}
CALL_KINDS = {"Redirect", "WrapOperation", "WrapWithCondition", "ModifyReceiver", "ModifyArg", "ModifyArgs",
              "ModifyExpressionValue", "ModifyVariable", "ModifyConstant"}
EVENT_DESC = "Lnet/fabricmc/fabric/api/event/Event;"
SEV = {"high": 0, "medium": 1, "low": 2}
CALLBACKS = ("CallbackInfo", "CallbackInfoReturnable")


class Names:
    """Mojang <-> intermediary names from the tiny file."""

    def __init__(self, path):
        self.c_int2moj, self.c_moj = {}, set()
        self.methods = collections.defaultdict(lambda: collections.defaultdict(set))  # moj class -> moj name -> {moj desc}
        self.m_int = {}                                                                # (int class, int name) -> moj name
        self.m_glob = collections.defaultdict(set)                                     # int name -> {moj name}
        cur_int = cur_moj = None
        for line in path.read_text(encoding="utf-8").splitlines():
            p = line.split("\t")
            if p[0] == "c":
                cur_moj, cur_int = p[1], p[2]
                self.c_int2moj[cur_int] = cur_moj
                self.c_moj.add(cur_moj)
            elif len(p) == 5 and p[1] == "m":
                self.methods[cur_moj][p[3]].add(p[2])
                self.m_int[(cur_int, p[4])] = p[3]
                self.m_glob[p[4]].add(p[3])

    def cls(self, n, moj):
        return n if moj else self.c_int2moj.get(n, n)

    def desc(self, d, moj):
        if moj or not d:
            return d
        return re.sub(r"L([^;]+);", lambda m: "L" + self.c_int2moj.get(m.group(1), m.group(1)) + ";", d)

    def method(self, owner, name, moj):
        if moj or not name.startswith("method_"):
            return name
        hit = self.m_int.get((owner, name))
        if hit:
            return hit
        g = self.m_glob.get(name)
        return sorted(g)[0] if g else name


REF = re.compile(r"^(?:L([^;]+);)?([^(;:]*)([(:].*)?$")


def split_ref(s):
    m = REF.match(s)
    return (m.group(1), m.group(2), m.group(3) or "") if m else (None, s, "")


class Refmap:
    def __init__(self, data):
        d = data or {}
        self.m = (d.get("data") or {}).get("named:intermediary") or d.get("mappings") or {}

    def get(self, mixin, s):
        return self.m.get(mixin, {}).get(s, s)


def ann_list(v):
    if v is None:
        return []
    return v if isinstance(v, list) else [v]


def anns(m, suffix):
    out = []
    for t, v in m.annots:
        if t.endswith(suffix):
            out.append(v)
        elif t.endswith("/Definitions;"):
            out += [d[1] for d in ann_list(v.get("value")) if isinstance(d, tuple)]
    return out


class Hook:
    __slots__ = ("cls", "name", "desc", "kind", "at", "handler", "mod", "group", "cancels", "sets_ret", "skips", "maybe", "events")

    def __init__(self, **kw):
        self.events = set()
        for k, v in kw.items():
            setattr(self, k, v)


def norm_target(names, s, mixin, refmap, moj, target_int):
    s = refmap.get(mixin, s)
    owner, name, rest = split_ref(s)
    if rest.startswith("("):
        name = names.method(owner or target_int, name, moj)
        rest = names.desc(rest, moj)
    elif rest.startswith(":"):
        rest = ":" + names.desc(rest[1:], moj)
    return ("L" + names.cls(owner, moj) + ";" if owner else "") + name + rest


def at_keys(m, mixin, kind, refmap, names, moj, target_int):
    out = set()
    defs = {n for d in anns(m, "/Definition;") for k in ("method", "field") for n in ann_list(d.get(k)) if isinstance(n, str)}
    for t, v in m.annots:
        if t.split("/")[-1][:-1] not in KINDS:
            continue
        ats = [a[1] for a in ann_list(v.get("at")) if isinstance(a, tuple)]
        if kind == "ModifyConstant":
            out.add("CONSTANT")
        elif kind in ("Overwrite", "WrapMethod"):
            out.add("METHOD")
        elif kind == "ModifyReturnValue" and not ats:
            out.add("RETURN")
        for a in ats:
            val, tgt = a.get("value") or "AT", a.get("target")
            if val == "MIXINEXTRAS:EXPRESSION":
                out |= {"DEF:" + norm_target(names, d, mixin, refmap, moj, target_int) for d in defs} or {"EXPR"}
            elif tgt:
                out.add(val + ":" + norm_target(names, tgt, mixin, refmap, moj, target_int))
            else:
                out.add(val)
    return out


def find_events(ci, m, lookup):
    events, seen, stack = set(), set(), [(ci, m, 0)]
    while stack:
        c, mm, d = stack.pop()
        if (c.name, mm.name, mm.desc) in seen:
            continue
        seen.add((c.name, mm.name, mm.desc))
        for kind, owner, name, desc in mm.fields:
            if kind == "gs" and desc == EVENT_DESC and owner.startswith("net/fabricmc/fabric/api/"):
                events.add(owner.rsplit("/", 1)[-1] + "." + name)
        if d >= 3:
            continue
        for kind, owner, name, desc in mm.calls:
            tc = c if owner == c.name else lookup(owner) if owner.startswith("net/fabricmc/") else None
            for t in (tc.methods if tc else ()):
                if t.name == name and t.desc == desc:
                    stack.append((tc, t, d + 1))
    return events


def class_hooks(ci, mixin, refmap, names, moj, mod, group, lookup, fabric):
    mx = ci.annot("Lorg/spongepowered/asm/mixin/Mixin;")
    if mx is None:
        return []
    targets = [n.strip("L;") if isinstance(n, str) else n[1][1:-1] for n in
               [("c", t[1]) for t in ann_list(mx.get("value")) if isinstance(t, tuple)]] + \
              [t.replace(".", "/") for t in ann_list(mx.get("targets")) if isinstance(t, str)]
    out = []
    for m in ci.methods:
        for t, v in m.annots:
            kind = t.split("/")[-1][:-1]
            if kind not in KINDS or ("mixin" not in t.lower() and "llamalad7" not in t):
                continue
            calls = [(o, n) for _k, o, n, _d in m.calls]
            cancels = any(o.endswith(CALLBACKS) and n in ("cancel", "setReturnValue") for o, n in calls)
            sets_ret = any(o.endswith("CallbackInfoReturnable") and n == "setReturnValue" for o, n in calls)
            keeps_original = any(o.endswith("/Operation") and n == "call" for o, n in calls)
            skips = (kind == "Inject" and cancels) or kind in ("WrapWithCondition", "Redirect", "Overwrite") \
                or (kind in ("WrapOperation", "WrapMethod") and not keeps_original)
            maybe = kind in ("WrapOperation", "WrapMethod") and keeps_original
            events = find_events(ci, m, lookup) if fabric and not moj else set()
            if kind == "Overwrite":
                strings = [m.name + m.desc if moj or m.name + m.desc in refmap.m.get(mixin, {}) else m.name]
            else:
                strings = [s for s in ann_list(v.get("method")) if isinstance(s, str)]
            for tcls_int in targets:
                tcls = names.cls(tcls_int, moj)
                if tcls not in names.c_moj:
                    continue
                at = at_keys(m, mixin, kind, refmap, names, moj, tcls_int)
                for s in strings:
                    s = refmap.get(mixin, s)
                    owner, name, rest = split_ref(s)
                    mname = names.method(owner or tcls_int, name, moj)
                    desc = names.desc(rest, moj) if rest.startswith("(") else None
                    cands = {d for d in names.methods[tcls].get(mname, ())}
                    cands = {d for d in cands if d == desc} if desc else cands
                    for d in (cands or {desc or ""}):
                        out.append(Hook(cls=tcls, name=mname, desc=d, kind=kind, at=at,
                                        handler=mixin.rsplit("/", 1)[-1] + "." + m.name, mod=mod, group=group,
                                        cancels=cancels, sets_ret=sets_ret, skips=skips, maybe=maybe, events=events))
    return out


def load_config(raw):
    try:
        return json.loads(raw)
    except ValueError:
        return json.loads(re.sub(r"^\s*//.*$|/\*.*?\*/", "", raw.decode("utf-8", "replace"), flags=re.S | re.M))


def config_classes(cfg):
    pkg = cfg.get("package", "").replace(".", "/")
    return [pkg + "/" + n.replace(".", "/") for k in ("mixins", "client", "server") for n in cfg.get(k, [])]


def jar_mods(zf, group, label):
    """Yield (zipfile, mod id, group) for a jar and its nested jars."""
    try:
        meta = json.loads(zf.read("fabric.mod.json").decode("utf-8-sig"), strict=False)
    except KeyError:
        yield zf, None, "neoforge"
        return
    mid = meta.get("id", label)
    if mid == "sporran":  # rename:keep: mod id of the old builds (1.0.8 and before); an installed old build is not another mod
        return
    grp = "nytheria_compat" if mid == "nytheria_compat" else group
    yield zf, mid, grp
    nested = [j["file"] for j in meta.get("jars", [])] or [n for n in zf.namelist() if n.startswith("META-INF/jars/") and n.endswith(".jar")]
    for n in nested:
        try:
            sub = zipfile.ZipFile(io.BytesIO(zf.read(n)))
        except (KeyError, zipfile.BadZipFile):
            continue
        yield from jar_mods(sub, "bundled" if grp == "nytheria_compat" else grp, n)


def collect_pack(mods_dir, names):
    fabric_classes, zips = {}, []
    for jar in sorted(mods_dir.glob("*.jar"), key=lambda p: (not p.name.startswith("fabric-api"), p.name)):
        for zf, mid, grp in jar_mods(zipfile.ZipFile(jar), "pack", jar.name):
            if mid and grp != "neoforge":
                zips.append((zf, mid, grp))
            if mid and mid.startswith("fabric"):
                for n in zf.namelist():
                    if n.startswith("net/fabricmc/") and n.endswith(".class"):
                        fabric_classes[n[:-6]] = (zf, n)
    cache = {}

    def lookup(owner):
        if owner not in cache:
            hit = fabric_classes.get(owner)
            cache[owner] = classfile.parse_class(hit[0].read(hit[1])) if hit else None
        return cache[owner]

    hooks = []
    for zf, mid, grp in zips:
        try:
            meta = json.loads(zf.read("fabric.mod.json").decode("utf-8-sig"), strict=False)
        except KeyError:
            continue
        for ent in meta.get("mixins", []):
            cfg_name = ent if isinstance(ent, str) else ent.get("config")
            try:
                cfg = load_config(zf.read(cfg_name))
            except (KeyError, ValueError):
                continue
            try:
                refmap = Refmap(json.loads(zf.read(cfg.get("refmap", "mixin.refmap.json"))))
            except (KeyError, ValueError):
                refmap = Refmap(None)
            for mixin in config_classes(cfg):
                try:
                    ci = classfile.parse_class(zf.read(mixin + ".class"))
                except KeyError:
                    continue
                hooks += class_hooks(ci, mixin, refmap, names, False, mid, grp, lookup, mid.startswith("fabric"))
    return hooks


def collect_nytheria_compat(names):
    hooks = []
    for cfg_path in NYTHERIACOMPAT_CONFIGS:
        cfg = load_config((ROOT / cfg_path).read_bytes())
        for mixin in config_classes(cfg):
            for root in CLASS_ROOTS:
                f = ROOT / root / (mixin + ".class")
                if f.exists():
                    ci = classfile.parse_class(f.read_bytes())
                    hooks += class_hooks(ci, mixin, Refmap(None), names, True, "nytheria_compat", "nytheria_compat", None, False)
                    break
    return hooks


def short(c):
    return c.rsplit("/", 1)[-1]


def analyse(sp, others, bundled):
    by = collections.defaultdict(lambda: ([], [], []))
    for i, hs in enumerate((sp, others, bundled)):
        for h in hs:
            by[(h.cls, h.name, h.desc)][i].append(h)
    rows = []
    for (cls, name, desc), (S, O, B) in by.items():
        if not S or not O:
            continue
        shared = {k for s in S for o in O for k in s.at & o.at}
        same_call = any(s.kind in CALL_KINDS and o.kind in CALL_KINDS and s.at & o.at - {"HEAD", "RETURN", "TAIL"}
                        for s in S for o in O)
        ret_kinds = ("ModifyReturnValue", "Overwrite") if desc.endswith(")V") else ("ModifyReturnValue", "WrapMethod", "Overwrite")
        both_ret = any(h.kind in ret_kinds or h.sets_ret for h in S) and any(h.kind in ret_kinds or h.sets_ret for h in O)
        both_wrap = any(h.kind == "WrapMethod" for h in S) and any(h.kind == "WrapMethod" for h in O)
        reason = sorted({f"{h.kind}({h.mod})" for h in S + O if h.kind in ("Overwrite", "Redirect")})
        reason += ["same call"] * same_call + ["both change return value"] * both_ret + ["both wrap method"] * both_wrap
        if reason:
            sev = "high"
        elif any(h.skips for h in S + O):
            sev, reason = "medium", sorted({f"{h.kind}({h.mod})" for h in S + O if h.skips})
        else:
            sev = "low"
        events = sorted({e for h in O for e in h.events})
        s_skips = sorted({h.kind for h in S if h.skips})
        s_maybe = sorted({h.kind for h in S if h.maybe})
        rows.append(dict(
            cls=cls.replace("/", "."), name=name, desc=desc, sev=sev, same_at="yes" if shared else "no",
            shared=";".join(sorted(shared)), reason=";".join(reason), tight=bool(shared - {"HEAD", "RETURN", "TAIL"}),
            nytheria_compat=";".join(sorted({f"{h.handler}[{h.kind}]" for h in S})),
            others=";".join(sorted({f"{m}:{','.join(sorted({h.kind for h in O if h.mod == m}))}" for m in {h.mod for h in O}})),
            n_others=len({h.mod for h in O}), events=";".join(events),
            nytheria_compat_skips=("yes:" + ",".join(s_skips)) if s_skips else ("conditional:" + ",".join(s_maybe)) if s_maybe else "no",
            event_skippable=("yes" if s_skips else "conditional" if s_maybe else "no") if events else "",
            bundled=";".join(sorted({h.mod for h in B})), n_nytheria_compat=len(S)))
    rows.sort(key=lambda r: (SEV[r["sev"]], not r["tight"], not r["events"], -r["n_others"], r["same_at"] != "yes", -r["n_nytheria_compat"],
                             r["cls"], r["name"]))
    return rows


COLS = ["rank", "class", "method", "desc", "severity", "reason", "same_at", "shared_at", "nytheria_compat_handlers", "other_mods", "n_other_mods",
        "fabric_events", "nytheria_compat_can_cancel_or_skip", "event_skippable", "bundled_libs"]


def write(rows, out):
    with open(out, "w", encoding="utf-8", newline="") as f:
        f.write("\t".join(COLS) + "\n")
        for i, r in enumerate(rows, 1):
            f.write("\t".join(str(x) for x in (i, r["cls"], r["name"], r["desc"], r["sev"], r["reason"], r["same_at"], r["shared"], r["nytheria_compat"],
                                               r["others"], r["n_others"], r["events"], r["nytheria_compat_skips"], r["event_skippable"],
                                               r["bundled"])) + "\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mods", type=Path, default=MODS)
    ap.add_argument("--out", type=Path, default=OUT)
    a = ap.parse_args()
    names = Names(TINY)
    pack = collect_pack(a.mods, names)
    sp = collect_nytheria_compat(names)
    rows = analyse(sp, [h for h in pack if h.group == "pack"], [h for h in pack if h.group == "bundled"])
    write(rows, a.out)
    per_mod = collections.Counter(m for r in rows for m in {x.split(":")[0] for x in r["others"].split(";")})
    print(f"nytheria_compat hooks {len(sp)}, pack hooks {len(pack)}, vanilla methods hooked by both {len(rows)} "
          f"(high {sum(r['sev'] == 'high' for r in rows)}, medium {sum(r['sev'] == 'medium' for r in rows)}, "
          f"low {sum(r['sev'] == 'low' for r in rows)}), wrote {a.out}")
    print("top mods:", ", ".join(f"{m} {n}" for m, n in per_mod.most_common(15)))


if __name__ == "__main__":
    main()
