#!/usr/bin/env python3
"""Every mixin of the Porting Lib modules that really load, and the removed Nytheria Compat hooks on the same target method. Dev tool, not shipped.

    py -3 tools/trim/portlib-overlap.py --jar build/libs/<ours>.jar --outdir DIR [--mods DIR]

Fabric Loader keeps the highest version of a jar-in-jar id, so the modules are picked across our jar and every pack jar, nested
jars included. Sporran-*.jar in the pack is skipped: our jar replaces it.
Reuses mixin-clash.py for reading mixins, so its limits apply: @At ordinal and shift are not read, config plugins are not run.
A hit is a hook-verdicts.tsv row whose handler is no longer in the tree and whose target class and method name match a Porting Lib
hook. The sheet has no descriptors, so overloads match too.

Writes portlib-modules.tsv, portlib-mixins.tsv and portlib-hits.tsv to DIR.
"""
import argparse
import collections
import csv
import importlib.util
import io
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("mixin_clash", Path(__file__).with_name("mixin-clash.py"))
clash = importlib.util.module_from_spec(spec)
sys.modules["mixin_clash"] = clash
spec.loader.exec_module(clash)

VERDICTS = ROOT / ".knowledge/sessions/2026-11-01_1404_lead-hook-verification/hook-verdicts.tsv"
SOURCE_ROOTS = ["src/main", "forge/src", "fml", "loader", "compat"]
SKIP_JAR = re.compile(r"^(sporran|nytheriacompat)-.*\.jar$", re.I)


def version_key(v):
    return tuple(int(n) for n in re.findall(r"\d+", v.split("+")[0]))


def modules(zf, top, found):
    try:
        meta = json.loads(zf.read("fabric.mod.json").decode("utf-8-sig"), strict=False)
    except KeyError:
        return
    if meta.get("id", "").startswith("porting_lib"):
        found[meta["id"]].append((version_key(meta["version"]), meta["version"], top, zf))
    for n in zf.namelist():
        if n.startswith("META-INF/jars/") and n.endswith(".jar"):
            modules(zipfile.ZipFile(io.BytesIO(zf.read(n))), top, found)


def winners(jar, mods_dir):
    found = collections.defaultdict(list)
    modules(zipfile.ZipFile(jar), "OURS", found)
    for p in sorted(mods_dir.glob("*.jar")):
        if not SKIP_JAR.match(p.name):
            modules(zipfile.ZipFile(p), p.name, found)
    return {mid: (max(c, key=lambda e: e[0]), sorted({e[1] for e in c})) for mid, c in found.items()}


def hooks_of(mid, zf, names):
    hooks, plain = [], []
    meta = json.loads(zf.read("fabric.mod.json").decode("utf-8-sig"), strict=False)
    for ent in meta.get("mixins", []):
        cfg_name = ent if isinstance(ent, str) else ent.get("config")
        cfg = clash.load_config(zf.read(cfg_name))
        if cfg.get("plugin"):
            print(f"{mid}: {cfg_name} has plugin {cfg['plugin']}")
        try:
            refmap = clash.Refmap(json.loads(zf.read(cfg.get("refmap", "mixin.refmap.json"))))
        except KeyError:
            refmap = clash.Refmap(None)
        for mixin in clash.config_classes(cfg):
            ci = clash.classfile.parse_class(zf.read(mixin + ".class"))
            found = clash.class_hooks(ci, mixin, refmap, names, False, mid, "bundled", lambda _: None, False)
            hooks += found
            if not found:
                plain.append(mixin)
    return hooks, plain


def tree_text():
    out = {}
    for top in SOURCE_ROOTS:
        for p in (ROOT / top).rglob("*"):
            if p.suffix in (".java", ".kt"):
                out[p.relative_to(ROOT).as_posix()] = p.read_text(encoding="utf-8", errors="replace")
    return out


def gone_rows():
    """A row is gone when its handler is not a method of its own file. The file may have moved, so a missing file falls back to its name."""
    src = tree_text()
    by_name = collections.defaultdict(list)
    for path in src:
        by_name[path.rsplit("/", 1)[-1]].append(path)
    rows = list(csv.DictReader(open(VERDICTS, encoding="utf-8"), delimiter="	", quoting=csv.QUOTE_NONE))
    gone = []
    for r in rows:
        call = re.compile(r"(?<![\w$])" + re.escape(r["handler"]) + r"\s*\(")
        paths = [r["file"]] if r["file"] in src else by_name[r["file"].rsplit("/", 1)[-1]]
        if not any(call.search(src[p]) for p in paths):
            gone.append(r)
    return rows, gone


def targets(cell):
    head, _, at = cell.partition(" @")
    classes, _, methods = head.partition("#")
    return {(c.rsplit(".", 1)[-1], m) for c in classes.split("|") for m in methods.split(",")}, at


def simple(cls):
    return cls.rsplit("/", 1)[-1]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jar", type=Path, required=True)
    ap.add_argument("--mods", type=Path, default=clash.MODS)
    ap.add_argument("--outdir", type=Path, required=True)
    a = ap.parse_args()
    names = clash.Names(clash.TINY)
    win = winners(a.jar, a.mods)
    by_target, all_hooks = collections.defaultdict(list), []
    with open(a.outdir / "portlib-modules.tsv", "w", encoding="utf-8", newline="") as f:
        f.write("module\twinner\tfrom\tall versions\thooks\tmixins without vanilla hooks\n")
        for mid in sorted(win):
            (_, version, top, zf), seen = win[mid]
            hooks, plain = hooks_of(mid, zf, names)
            f.write(f"{mid}\t{version}\t{top}\t{','.join(seen)}\t{len(hooks)}\t{len(plain)}\n")
            all_hooks += hooks
            for h in hooks:
                by_target[(simple(h.cls), h.name)].append(h)
    with open(a.outdir / "portlib-mixins.tsv", "w", encoding="utf-8", newline="") as f:
        f.write("module\tmixin.handler\tkind\tclass\tmethod\tdesc\tat\n")
        for h in sorted(all_hooks, key=lambda h: (h.cls, h.name, h.desc, h.mod, h.handler)):
            f.write("\t".join((h.mod, h.handler, h.kind, h.cls, h.name, h.desc, ";".join(sorted(h.at)))) + "\n")
    rows, gone = gone_rows()
    hits = 0
    with open(a.outdir / "portlib-hits.tsv", "w", encoding="utf-8", newline="") as f:
        f.write("verdict\tfile\thandler\ttarget method\tportlib module\tportlib handler\tkind\tdesc\tportlib at\tsame at\n")
        for r in gone:
            keys, at = targets(r["target method"])
            for key in sorted(keys):
                for h in by_target.get(key, ()):
                    same = "yes" if any(a.split(".")[-1] in k for a in at.split(",") if a for k in h.at) else ""
                    f.write("\t".join((r["verdict"], r["file"].rsplit("/", 1)[-1], r["handler"], r["target method"], h.mod, h.handler,
                                       h.kind, h.desc, ";".join(sorted(h.at)), same)) + "\n")
                    hits += 1
    kinds = collections.Counter(r["verdict"] for r in gone)
    print(f"modules {len(win)}, hooks {len(all_hooks)}, verdict rows {len(rows)}, gone {dict(kinds)}, hit lines {hits}")


if __name__ == "__main__":
    main()
