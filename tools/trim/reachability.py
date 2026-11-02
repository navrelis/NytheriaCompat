#!/usr/bin/env python3
"""Nytheria Compat trim: reachability / dead-code proof tool (dev only, Python 3 stdlib, not part of the jar).

Usage (from the repo root, after `./gradlew remapJar`; dev classes under build/classes stay Mojang-mapped):

    py tools/trim/reachability.py --originals <originals-folder> --out <folder>

Writes <folder>/reach-report.md (short tables) and <folder>/reach.json (full data) in one run.
Options: --classes DIR (repeatable, default: every build/classes/{java,kotlin}/main under the repo root, compat/*, loader/*),
         --repo DIR (source/resources root, default: the repo containing this file), --needs FILE (N1 needs.json for the
         reconciliation, optional), --cache DIR (pickle cache of parsed class files, speeds up re-runs).

Inputs : Nytheria Compat's compiled classes + the four target jars (Cataclysm, Astral, Curios, Lionfish; GeckoLib is only
         reported as an extra consumer), Nytheria Compat's fabric.mod.json, mixin configs, services, class transformers, N1 needs.json.
Report : A unreachable classes, B events (subscribers/post sites), C mixin verdicts, D extension-interface methods.
Rule   : anything reachable by reflection or a string constant that this tool cannot rule out is `uncertain`, never `dead`.
"""
import argparse
import collections
import hashlib
import json
import os
import pickle
import re
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from classfile import ClassInfo, MethodInfo, parse_class, desc_classes, ACC_INTERFACE, ACC_ABSTRACT, ACC_STATIC  # noqa: E402

# ----------------------------------------------------------------------------------------------------------- loading

UNIVERSE_PREFIXES = ("net/neoforged/", "cpw/", "dev/nytheria/compat/")
EVENT_BASE = "net/neoforged/bus/api/Event"
SUBSCRIBE = "Lnet/neoforged/bus/api/SubscribeEvent;"
MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;"
MIXIN_REF_FOLDS = ("kotlin/Metadata",)


def log(*a):
    print(*a, file=sys.stderr, flush=True)


def load_dir(root, cache):
    out = {}
    for d, _, fs in os.walk(root):
        for f in fs:
            if f.endswith(".class"):
                p = os.path.join(d, f)
                ci = cached_parse(open(p, "rb").read(), cache)
                ci.path = p
                out[ci.name] = ci
    return out


def load_jar(path, cache):
    out = {}
    with zipfile.ZipFile(path) as z:
        for n in z.namelist():
            if n.endswith(".class") and not n.startswith("META-INF/"):
                ci = cached_parse(z.read(n), cache)
                ci.path = path + "!" + n
                out[ci.name] = ci
    return out


def cached_parse(data, cache):
    if not cache:
        return parse_class(data)
    key = os.path.join(cache, hashlib.sha1(data + b'parser-v2').hexdigest())
    try:
        with open(key, "rb") as f:
            return pickle.load(f)
    except OSError:
        pass
    ci = parse_class(data)
    with open(key, "wb") as f:
        pickle.dump(ci, f, protocol=pickle.HIGHEST_PROTOCOL)
    return ci


# ----------------------------------------------------------------------------------------------------------- model

POST_NAMES = {("net/neoforged/bus/api/IEventBus", "post"),
              ("net/neoforged/fml/ModLoader", "postEvent"),
              ("net/neoforged/fml/ModLoader", "postEventWithReturn"),
              ("net/neoforged/fml/ModLoader", "postEventWrapContainerInModOrder"),
              ("net/neoforged/fml/ModLoader", "postEventWithWrapInModOrder"),
              ("net/neoforged/fml/ModLoader", "dispatchParallelEvent"),
              ("net/neoforged/fml/ModLoader", "runEventGenerator")}
LISTENER_NAMES = {"addListener", "addGenericListener", "register"}
IGNORED_EVENT_TYPES = {EVENT_BASE, "net/neoforged/bus/api/ICancellableEvent"}


class Model:
    def __init__(self, cls):
        self.cls = cls
        self.subs = collections.defaultdict(list)
        for c in cls.values():
            if c.super:
                self.subs[c.super].append(c.name)
            for i in c.interfaces:
                self.subs[i].append(c.name)
        self._anc = {}
        self._mi = {}
        self.universe = {n for n in cls if n.startswith(UNIVERSE_PREFIXES)}

    def ancestors(self, name):
        """All supertypes of a class (names, incl. ones outside the tree), not including the class itself."""
        r = self._anc.get(name)
        if r is None:
            r = set()
            self._anc[name] = r
            c = self.cls.get(name)
            if c:
                for s in ([c.super] if c.super else []) + c.interfaces:
                    r.add(s)
                    r |= self.ancestors(s)
        return r

    def descendants(self, name):
        out, todo = set(), [name]
        while todo:
            for s in self.subs.get(todo.pop(), ()):
                if s not in out:
                    out.add(s)
                    todo.append(s)
        return out

    def is_event(self, name):
        return name == EVENT_BASE or EVENT_BASE in self.ancestors(name)

    def method(self, cname, mname, desc):
        c = self.cls.get(cname)
        if c:
            for m in c.methods:
                if m.name == mname and m.desc == desc:
                    return m
        return None

    def resolve(self, owner, name, desc):
        """Declaring (class, method) pairs a call may land in: the declaration found upwards plus all overriders (CHA)."""
        key = (owner, name, desc)
        r = self._mi.get(key)
        if r is not None:
            return r
        r = []
        seen, todo = set(), [owner]
        found = False
        while todo and not found:  # upwards: class chain first, then interfaces, breadth first
            nxt = []
            for n in todo:
                if n in seen:
                    continue
                seen.add(n)
                m = self.method(n, name, desc)
                if m is not None:
                    r.append((n, m))
                    found = True
                c = self.cls.get(n)
                if c:
                    nxt += ([c.super] if c.super else []) + c.interfaces
            todo = nxt
        if name != "<init>" and name != "<clinit>":
            for d in self.descendants(owner):
                m = self.method(d, name, desc)
                if m is not None:
                    r.append((d, m))
        self._mi[key] = r
        return r


def jar_event_types(model, jar_classes):
    """Event subscriptions in a set of classes: [(event, class, method, how)]."""
    out = []
    for c in jar_classes.values():
        for m in c.methods:
            is_sub = any(t == SUBSCRIBE for t, _ in m.annots)
            if is_sub:
                for p in desc_classes(m.desc.split(")")[0]):
                    if model.is_event(p):
                        out.append((p, c.name, m.name, "@SubscribeEvent"))
                continue
            if any(k in ("v", "i") and nm in LISTENER_NAMES and (o.endswith("IEventBus") or o.endswith("EventBus"))
                   for k, o, nm, _d in m.calls):
                cand = set(x for x in m.classes if model.is_event(x))
                for k, o, nm, d in m.calls:
                    if k == "h":
                        cand.update(x for x in desc_classes(d.split(")")[0]) if model.is_event(x))
                    elif k in ("v", "i", "s", "p"):
                        pass
                for ev in cand:
                    out.append((ev, c.name, m.name, "addListener"))
    return out


# ----------------------------------------------------------------------------------------------------- repo inputs

SRC_ROOTS = ["src/main/java", "src/main/kotlin", "forge/src/main/java", "forge/coremods/src/main/java", "fml/loader/src/main/java",
             "loader/src/main/java", "loader/src/main/kotlin", "loader/fabric/src/main/java", "loader/fabric/src/main/kotlin"]
RES_ROOTS = ["src/main/resources", "forge/src/main/resources", "forge/coremods/src/main/resources", "fml/loader/src/main/resources",
             "loader/src/main/resources", "loader/fabric/src/main/resources"]


def module_dirs(repo):
    """[(label, module_dir)] of every shipped module whose classes we analyse. 'main' = everything in the shipped jar."""
    mods = [("main", repo)]
    for sub in ("loader", os.path.join("loader", "fabric")):
        mods.append(("main", os.path.join(repo, sub)))
    cdir = os.path.join(repo, "compat")
    if os.path.isdir(cdir):
        for n in sorted(os.listdir(cdir)):
            if os.path.isdir(os.path.join(cdir, n)):
                mods.append(("compat:" + n, os.path.join(cdir, n)))
    return mods


def default_class_dirs(repo):
    out = []
    for label, d in module_dirs(repo):
        for lang in ("java", "kotlin"):
            p = os.path.join(d, "build", "classes", lang, "main")
            if os.path.isdir(p):
                out.append((label, p))
    return out


def slash(n):
    return n.replace(".", "/")


def read_json(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def read_resource_roots(repo):
    """Entry points / services / transformers / mixin configs from Nytheria Compat's resources. Class names in internal form."""
    r = {"entry": collections.defaultdict(set), "services": collections.defaultdict(set), "transform": collections.defaultdict(set),
         "plugins": collections.defaultdict(set), "configs": []}
    res_dirs = [("main", os.path.join(repo, p)) for p in RES_ROOTS]
    cdir = os.path.join(repo, "compat")
    if os.path.isdir(cdir):
        for n in sorted(os.listdir(cdir)):
            res_dirs.append(("compat:" + n, os.path.join(cdir, n, "src", "main", "resources")))
    for label, d in res_dirs:
        if not os.path.isdir(d):
            continue
        fmj = os.path.join(d, "fabric.mod.json")
        if os.path.isfile(fmj):
            j = read_json(fmj)
            for key, lst in (j.get("entrypoints") or {}).items():
                for e in lst:
                    v = e["value"] if isinstance(e, dict) else e
                    r["entry"][label].add(slash(v.split("::")[0]))
        sd = os.path.join(d, "META-INF", "services")
        if os.path.isdir(sd):
            for n in os.listdir(sd):
                with open(os.path.join(sd, n), encoding="utf-8") as f:
                    for line in f:
                        line = line.split("#")[0].strip()
                        if line:
                            r["services"][label].add(slash(line))
                r["services"][label].add(slash(n))  # the service interface itself is loaded by ServiceLoader.load(X.class) callers
        for fn in os.listdir(d):
            p = os.path.join(d, fn)
            if fn.endswith(".classtransform.json"):
                j = read_json(p)
                pkg = j.get("package", "")
                for key in ("java", "mixins"):
                    for n in j.get(key, []) or []:
                        r["transform"][label].add(slash(pkg + "." + n))
            elif fn.endswith(".json") and fn.endswith("mixins.json"):
                j = read_json(p)
                pkg = j.get("package", "")
                names = [slash(pkg + "." + n) for k in ("mixins", "client", "server") for n in j.get(k, []) or []]
                r["configs"].append({"file": os.path.relpath(p, repo).replace("\\", "/"), "module": label, "package": pkg, "classes": names,
                                     "plugin": slash(j["plugin"]) if j.get("plugin") else None})
                if j.get("plugin"):
                    r["plugins"][label].add(slash(j["plugin"]))
    return r


def count_loc(path):
    """Non-blank, non-comment lines of a .java/.kt file."""
    n, in_block = 0, False
    try:
        with open(path, encoding="utf-8", errors="replace") as f:
            for line in f:
                s = line.strip()
                if in_block:
                    if "*/" in s:
                        in_block = False
                        s = s.split("*/", 1)[1].strip()
                    else:
                        continue
                if not s or s.startswith("//"):
                    continue
                if s.startswith("/*"):
                    if "*/" not in s:
                        in_block = True
                        continue
                    s = s.split("*/", 1)[1].strip()
                    if not s:
                        continue
                n += 1
    except OSError:
        return 0
    return n


class Loc:
    """Source lines per class: the source file's lines are attributed to its primary (outermost, file-named) class."""

    def __init__(self, repo, cls):
        self.index = {}
        for sr in SRC_ROOTS:
            base = os.path.join(repo, sr)
            for d, _, fs in os.walk(base):
                for f in fs:
                    if f.endswith((".java", ".kt")):
                        self.index.setdefault(os.path.relpath(os.path.join(d, f), base).replace("\\", "/"), os.path.join(d, f))
        self.loc = {}
        files = {}
        for c in cls.values():
            if c.source and "$" not in c.name:
                rel = (c.name.rsplit("/", 1)[0] + "/" if "/" in c.name else "") + c.source
                p = self.index.get(rel)
                if p:
                    files.setdefault(rel, []).append(c.name)
        for rel, names in files.items():
            stem = rel.rsplit("/", 1)[-1].rsplit(".", 1)[0]
            primary = next((n for n in names if n.rsplit("/", 1)[-1] == stem), sorted(names)[0])
            self.loc[primary] = count_loc(self.index[rel])
        self.files_found = len(files)

    def of(self, name):
        return self.loc.get(name, 0)


# ----------------------------------------------------------------------------------------------------------- analysis

HANDLER_ANN = {"Inject", "Redirect", "ModifyArg", "ModifyArgs", "ModifyVariable", "ModifyConstant", "Overwrite",
               "ModifyExpressionValue", "ModifyReceiver", "ModifyReturnValue", "WrapOperation", "WrapWithCondition", "WrapMethod"}
VALUE_HANDLER = HANDLER_ANN - {"Inject", "WrapWithCondition"}  # replace a value of vanilla
ACCESS_ANN = {"Accessor", "Invoker"}
OTHER_ANN = {"Shadow", "Final", "Mutable", "Unique", "Intrinsic", "Coerce", "Dynamic", "Debug", "Pseudo", "Implements"}
NEUTRAL_PREFIX = ("get", "is", "has", "can", "should", "contains", "size", "level", "position", "blockPosition", "registryAccess",
                  "isEmpty", "x", "y", "z", "of", "copy", "asItem", "value", "key", "location", "holder", "ordinal", "name", "equals",
                  "hashCode", "toString", "stream", "orElse", "unwrap", "test", "apply", "length", "keySet", "values", "entrySet")
FLOW_METHODS = {"cancel", "setReturnValue"}  # CallbackInfo
EXT_IFACE_RE = re.compile(r"^I\w*Extensions?$")
MAX_DEPTH = 8


def ann_name(desc):
    return desc.rstrip(";").rsplit("/", 1)[-1].rsplit("$", 1)[-1]


class Analysis:
    def __init__(self, repo, cls, origin, jars, resources, loc):
        self.repo, self.cls, self.origin, self.jars, self.res, self.loc = repo, cls, origin, jars, resources, loc
        self.M = Model(cls)
        self.names = set(cls)
        self.universe = {n for n in self.M.universe if origin.get(n, "main") == "main"}
        self.events = sorted(n for n in cls if n != EVENT_BASE and self.M.is_event(n))
        self.eventset = set(self.events)
        self._prep_strings()
        self._prep_mixins()
        self._prep_calls()
        self._prep_events()

    # ---- strings / reflection hints
    def _class_from_string(self, s):
        if "/" in s:
            c = s
        elif "." in s:
            c = s.replace(".", "/")
        else:
            return None
        return c if c in self.names else None

    def _prep_strings(self):
        self.str_refs = collections.defaultdict(set)   # class -> Nytheria Compat classes named by a string constant
        self.prefix_strings = collections.defaultdict(set)  # package-prefix string (>= 4 segments) -> classes containing it
        self.broad_prefix_strings = collections.defaultdict(set)  # shorter prefixes: too broad to mean anything, only listed
        for c in self.cls.values():
            for s in c.strings:
                t = self._class_from_string(s)
                if t and t != c.name:
                    self.str_refs[c.name].add(t)
                elif s.endswith(("/", ".")) and s.replace(".", "/").startswith(UNIVERSE_PREFIXES):
                    pre = s.replace(".", "/")
                    (self.prefix_strings if pre.count("/") >= 4 else self.broad_prefix_strings)[pre].add(c.name)
            for s in c.annot_strings:
                for x in desc_classes(s):
                    if x in self.names and x != c.name:
                        self.str_refs[c.name].add(x)
        self.jar_str_refs = collections.defaultdict(set)
        for label, jc in self.jars.items():
            for c in jc.values():
                for s in c.strings:
                    t = self._class_from_string(s)
                    if t:
                        self.jar_str_refs[label].add(t)
        self.named_by_string = set()  # classes some string constant spells out
        for v in list(self.str_refs.values()) + list(self.jar_str_refs.values()):
            self.named_by_string |= v

    # ---- mixins
    def _prep_mixins(self):
        self.mixins = {}
        listed = {}
        for cfg in self.res["configs"]:
            for n in cfg["classes"]:
                listed[n] = cfg
        for n, c in self.cls.items():
            a = c.annot(MIXIN)
            if a is None:
                continue
            targets = [t[1][1:-1] for t in (a.get("value") or []) if isinstance(t, tuple) and t[0] == "class"]
            targets += [slash(t) for t in (a.get("targets") or []) if isinstance(t, str)]
            self.mixins[n] = {"targets": targets, "config": (listed.get(n) or {}).get("file"), "origin": self.origin.get(n, "main")}
        self.mixin_listed_missing = sorted(set(listed) - set(self.mixins))

    # ---- call indices
    def _prep_calls(self):
        self.callers = collections.defaultdict(set)        # (name, desc) -> classes calling/accessing it (any owner)
        self.callers_exact = collections.defaultdict(set)  # (owner, name, desc) -> classes
        for c in self.cls.values():
            for m in c.methods:
                for k, o, n, d in m.calls:
                    if k != "d":
                        self.callers[(n, d)].add(c.name)
                        self.callers_exact[(o, n, d)].add(c.name)
                for k, o, n, d in m.fields:
                    self.callers[(n, d)].add(c.name)
                    self.callers_exact[(o, n, d)].add(c.name)
        self.jar_sigs = collections.defaultdict(set)      # (name, desc) -> {jar labels} that call/access it
        self.jar_decl = collections.defaultdict(set)      # (name, desc) -> {jar labels} that declare it (override candidates)
        self.jar_mentions = collections.defaultdict(set)  # class -> {jar labels} mentioning it
        for label, jc in self.jars.items():
            for c in jc.values():
                for r in c.refs:
                    if r in self.names:
                        self.jar_mentions[r].add(label)
                for m in c.methods:
                    if not (m.access & (ACC_STATIC | 0x0002)) and not m.name.startswith("<"):
                        self.jar_decl[(m.name, m.desc)].add(label)
                    for k, o, n, d in m.calls:
                        if k != "d":
                            self.jar_sigs[(n, d)].add(label)
                    for k, o, n, d in m.fields:
                        self.jar_sigs[(n, d)].add(label)

    # ---- events
    def _prep_events(self):
        M = self.M
        self.sub_jar = collections.defaultdict(lambda: collections.defaultdict(set))  # type -> label -> {(class, method, how)}
        for label, jc in self.jars.items():
            for e, cn, mn, how in jar_event_types(M, jc):
                self.sub_jar[e][label].add((cn, mn, how))
        self.sub_internal = collections.defaultdict(list)  # class -> [(event, method, how)]
        self.universal_internal = []
        for cn, c in self.cls.items():
            for e, _cn, mn, how in jar_event_types(M, {cn: c}):
                if e == EVENT_BASE:
                    self.universal_internal.append((cn, mn, how))
                else:
                    self.sub_internal[cn].append((e, mn, how))
        self.post_sites = collections.defaultdict(list)  # event -> [(class, method)]
        self.post_methods = {}                           # (class, method, desc) -> set(events)
        for cn, c in self.cls.items():
            for m in c.methods:
                if any((o, n) in POST_NAMES for k, o, n, d in m.calls if k != "d"):
                    evs = self.event_mentions(c, m)
                    self.post_methods[(cn, m.name, m.desc)] = evs
                    for e in evs:
                        self.post_sites[e].append((cn, m.name))

    def event_mentions(self, c, m, depth=0):
        names = set(m.classes) | set(desc_classes(m.desc))
        for k, o, n, d in m.calls:
            names.add(o)
            names.update(desc_classes(d))
        for k, o, n, d in m.fields:
            names.add(o)
            names.update(desc_classes(d))
        evs = {x for x in names if x in self.eventset}
        if depth < 2:
            for k, o, n, d in m.calls:
                if k == "h" and o == c.name:
                    mm = self.M.method(o, n, d)
                    if mm is not None and mm is not m:
                        evs |= self.event_mentions(c, mm, depth + 1)
        return evs

    def subscribers_of(self, ev, live):
        """Subscribers that receive `ev`: subscription type == ev or a superclass of ev."""
        anc = self.M.ancestors(ev) | {ev}
        jars = {}
        for t in anc:
            for label, items in self.sub_jar.get(t, {}).items():
                jars.setdefault(label, []).extend(sorted((t,) + i for i in items))
        internal = []
        for cn in live:
            for e, mn, how in self.sub_internal.get(cn, ()):
                if e in anc:
                    internal.append((cn, mn, how, e))
        return {"jars": jars, "internal": internal}

    # ---- handler effects (post-only analysis)
    def effects(self, cname, m, depth=0, stack=None):
        """(events posted, other effects, flow effects) of a method, following calls into the Nytheria Compat tree."""
        key = (cname, m.name, m.desc)
        memo = self.__dict__.setdefault("_eff", {})
        if key in memo:
            return memo[key]
        stack = stack or set()
        if key in stack or depth > MAX_DEPTH:
            return (set(), [], False)
        stack = stack | {key}
        c = self.cls[cname]
        events, other, flow = set(), [], False
        for k, o, n, d in m.calls:
            if k == "d":
                continue
            if (o, n) in POST_NAMES:
                events |= self.event_mentions(c, m) or {"?"}
                continue
            if o in self.eventset or o in IGNORED_EVENT_TYPES:
                continue
            if o in self.cls and not o.startswith("net/minecraft"):
                tg = [(tc, tm) for tc, tm in self.M.resolve(o, n, d) if not (tm.access & ACC_ABSTRACT)]
                if tg:
                    if len(tg) > 8:
                        other.append("wide dispatch %s.%s" % (o, n))
                        continue
                    for tc, tm in tg:
                        e2, o2, f2 = self.effects(tc, tm, depth + 1, stack)
                        events |= e2
                        other += o2
                        flow |= f2
                    continue
            if o.startswith(("java/", "javax/")):
                continue
            if o.startswith(("org/spongepowered/", "com/llamalad7/")):
                if n in FLOW_METHODS or n == "set":
                    flow = True
                continue
            if n.startswith(NEUTRAL_PREFIX) or n == "<init>":
                continue
            other.append("%s.%s" % (o.rsplit("/", 1)[-1], n))
        for k, o, n, d in m.fields:
            if k in ("p", "ps") and o not in self.eventset:
                other.append("write %s.%s" % (o.rsplit("/", 1)[-1], n))
        res = (events, other, flow)
        memo[key] = res
        return res


    # ---- class graph / reachability
    def edges(self, n):
        memo = self.__dict__.setdefault("_edges", {})
        e = memo.get(n)
        if e is None:
            c = self.cls[n]
            e = {r for r in c.refs if r in self.names} | self.str_refs.get(n, set())
            e.discard(n)
            memo[n] = e
        return e

    def closure(self, roots, alive):
        seen, stack = set(), [r for r in roots if r in self.names and (r not in self.mixins or r in alive)]
        while stack:
            n = stack.pop()
            if n in seen:
                continue
            seen.add(n)
            for t in self.edges(n):
                if t in self.mixins and t not in alive:
                    continue
                stack.append(t)
        return seen

    def root_groups(self, alive):
        res = self.res
        main_entry = set(res["entry"].get("main", ())) | set(res["plugins"].get("main", ())) | set(res["transform"].get("main", ()))
        jar_roots = set()
        for label, jc in self.jars.items():
            if label == "gecko":
                continue
            for c in jc.values():
                jar_roots |= {r for r in c.refs if r in self.names}
            jar_roots |= self.jar_str_refs[label]
        compat = set()
        for label in res["entry"]:
            if label.startswith("compat:"):
                compat |= res["entry"][label] | res["services"][label] | res["plugins"][label] | res["transform"][label]
        compat |= {n for n, o in self.origin.items() if o.startswith("compat:")}
        gecko = set()
        for c in self.jars.get("gecko", {}).values():
            gecko |= {r for r in c.refs if r in self.names}
        return {"jars": jar_roots, "entry": main_entry, "services": set(res["services"].get("main", ())),
                "mixins": {m for m in alive if self.mixins[m]["origin"] == "main"}, "compat": compat, "geckolib": gecko}

    def reach(self, alive):
        groups = self.root_groups(alive)
        by = collections.defaultdict(set)
        for g, roots in groups.items():
            for n in self.closure(roots, alive):
                by[n].add(g)
        return by

    # ---- method-level reachability (A2): which methods and classes stay once dead mixin members are removed
    def method_reach(self, alive, verdicts):
        cls, M = self.cls, self.M
        live_m, live_c = set(), set()
        work = []
        trivial = {"java/lang/Object", "java/lang/Enum", "java/lang/Record", "java/lang/annotation/Annotation"}

        def usable(cn):
            return cn in cls and (cn not in self.mixins or cn in alive)

        def touch(cn, init=False):
            if not usable(cn):
                return
            if cn not in live_c:
                live_c.add(cn)
                work.append(("class", cn))
            if init:
                mark(cn, "<clinit>", "()V")

        def mark(cn, name, desc):
            key = (cn, name, desc)
            if key in live_m or not usable(cn) or M.method(cn, name, desc) is None:
                return
            live_m.add(key)
            work.append(("method", key))

        def touch_all(cn):
            """Class named by string/entry/service: constructor, static init and every non-private method are potential entry points."""
            if not usable(cn):
                return
            touch(cn, True)
            for m in cls[cn].methods:
                if not (m.access & 0x0002) and not (m.access & ACC_ABSTRACT):
                    mark(cn, m.name, m.desc)

        def type_refs(desc):
            for x in desc_classes(desc):
                touch(x)

        for label, jc in self.jars.items():
            if label == "gecko":
                continue
            for c in jc.values():
                for r in c.refs:
                    touch(r)
                for m in c.methods:
                    self._calls_to_roots(m, touch, mark)
            for t in self.jar_str_refs[label]:
                touch_all(t)
        res = self.res
        for grp in ("entry", "plugins", "transform", "services"):
            for n in res[grp].get("main", ()):
                touch_all(n)
        for n, c in cls.items():  # classes FML/Mixin load by annotation scanning
            if self.origin.get(n, "main") == "main" and any(t in ("Lnet/neoforged/fml/common/EventBusSubscriber;", "Lnet/neoforged/fml/common/Mod;") for t, _ in c.annots):
                touch_all(n)
        for mn in alive:
            if self.mixins[mn]["origin"] != "main":
                continue
            touch(mn)
            v = verdicts.get(mn) if verdicts else None
            if v is None:
                for m in cls[mn].methods:
                    if not m.name.startswith("<") and not (m.access & ACC_ABSTRACT):
                        mark(mn, m.name, m.desc)
                for i in cls[mn].interfaces:
                    touch(i)
            else:
                for mem in v["members"]:
                    if mem["verdict"] == "dead":
                        continue
                    if mem["kind"] in ("handler", "method", "accessor"):
                        mark(mn, mem["name"], mem["desc"])
                    elif mem["kind"] == "interface":
                        touch(mem["name"])
        # extension-interface methods reached by sig from code on vanilla types
        ext_sig = self.ext_sig_index(alive)
        for (n, d), lst in ext_sig.items():
            if (n, d) in self.jar_sigs or (n, d) in self.jar_decl:
                for i, m in lst:
                    mark(i, m.name, m.desc)
        while work:
            kind, x = work.pop()
            if kind == "class":
                c = cls[x]
                for s in ([c.super] if c.super else []) + c.interfaces:
                    touch(s)
                for f in c.fields:
                    type_refs(f[1])
                    for t, _ in f[3]:
                        type_refs(t)
                for t, _ in c.annots:
                    type_refs(t)
                if c.signature:
                    type_refs(c.signature)
                for inner, outer, _s, _f in c.inner:
                    if inner == x and outer:
                        touch(outer)
                if c.nest_host:
                    touch(c.nest_host)
                for t in self.str_refs.get(x, ()):
                    touch_all(t)
                for m in c.methods:
                    if any(t == SUBSCRIBE for t, _ in m.annots):
                        mark(x, m.name, m.desc)
                ext = [s for s in ([c.super] if c.super else []) + c.interfaces if s not in cls and s not in trivial]
                if ext and not c.is_interface:
                    for m in c.methods:
                        if not (m.access & (0x0002 | ACC_STATIC | ACC_ABSTRACT)) and not m.name.startswith("<"):
                            mark(x, m.name, m.desc)
            else:
                cn, name, desc = x
                m = M.method(cn, name, desc)
                touch(cn)
                type_refs(m.desc)
                for t, _ in m.annots:
                    type_refs(t)
                for k in m.classes:
                    touch(k)
                self._calls_to_roots(m, touch, mark, ext_sig)
        return live_m, live_c

    def _calls_to_roots(self, m, touch, mark, ext_sig=None):
        M = self.M
        for k, o, n, d in m.calls:
            for x in desc_classes(d):
                touch(x)
            if k == "d":
                continue
            touch(o, k == "s" or n == "<init>")
            tg = M.resolve(o, n, d) if o in self.cls else []
            for tc, tm in tg:
                touch(tc, k == "s" or n == "<init>")
                mark(tc, tm.name, tm.desc)
            if not tg and ext_sig is not None:
                for i, em in ext_sig.get((n, d), ()):
                    mark(i, em.name, em.desc)
        for k, o, n, d in m.fields:
            touch(o, k in ("gs", "ps"))
            for x in desc_classes(d):
                touch(x)

    def ext_sig_index(self, alive):
        """(name, desc) -> [(interface, method)] for interfaces that alive mixins add to vanilla types and for I*Extension interfaces."""
        ifaces, _ = self._ifaces
        todo = set(ifaces)
        for mn in alive:
            todo.update(self.cls[mn].interfaces)
        idx, seen = collections.defaultdict(list), set()
        for i in todo:
            for ic, m in self.iface_methods(i):
                if (ic, m.name, m.desc) not in seen:
                    seen.add((ic, m.name, m.desc))
                    idx[(m.name, m.desc)].append((ic, m))
        return idx

    def live_callers_of(self, live_m):
        out = collections.defaultdict(set)
        for (cn, name, desc) in live_m:
            m = self.M.method(cn, name, desc)
            for k, o, n, d in m.calls:
                if k != "d":
                    out[(n, d)].add(cn)
            for k, o, n, d in m.fields:
                out[(n, d)].add(cn)
        return out

    # ---- mixin judging
    def ext_ifaces(self):
        impl = set()
        for mn in self.mixins:
            impl.update(i for i in self.cls[mn].interfaces if i in self.cls)
        return {n for n, c in self.cls.items() if c.is_interface and self.origin.get(n, "main") == "main" and
                (EXT_IFACE_RE.match(n.rsplit("/", 1)[-1]) or n in impl)}, impl

    def iface_methods(self, i, seen=None):
        seen = seen if seen is not None else set()
        if i in seen or i not in self.cls:
            return []
        seen.add(i)
        c = self.cls[i]
        out = [(i, m) for m in c.methods if not m.name.startswith("<") and not (m.access & 0x1002)]
        for s in c.interfaces:
            out += self.iface_methods(s, seen)
        return out

    def used_sig(self, name, desc, live_other):
        """Who uses a method signature: jars (call or override) and live Nytheria Compat classes other than the mixin itself."""
        jar = self.jar_sigs.get((name, desc), set()) | self.jar_decl.get((name, desc), set())
        internal = [x for x in self.live_callers.get((name, desc), ()) if x in live_other]
        return jar, internal

    def judge_mixin(self, mn, live, ev_cache):
        c = self.cls[mn]
        live_other = live - {mn}
        ifaces, _impl = self._ifaces
        members = []

        def add(kind, name, desc, verdict, reason, **kw):
            d = {"kind": kind, "name": name, "desc": desc, "verdict": verdict, "reason": reason}
            d.update(kw)
            members.append(d)

        for m in c.methods:
            anns = {ann_name(t) for t, _ in m.annots}
            shadow = "Shadow" in anns
            if anns & HANDLER_ANN:
                evs, other, flow = self.effects(mn, m)
                value = bool(anns & VALUE_HANDLER)
                subbed = {}
                for e in evs:
                    if e == "?":
                        subbed[e] = "unknown event type"
                        continue
                    if e not in ev_cache:
                        ev_cache[e] = self.subscribers_of(e, live)
                    s = ev_cache[e]
                    if s["jars"] or s["internal"]:
                        subbed[e] = sorted(s["jars"]) + (["nytheria_compat"] if s["internal"] else [])
                if evs and not other:
                    if not subbed:
                        add("handler", m.name, m.desc, "dead",
                            "posts only events nobody subscribes to: " + ", ".join(sorted(e.rsplit("/", 1)[-1] for e in evs)),
                            confidence="medium" if (flow or value) else "high", events=sorted(evs))
                    else:
                        add("handler", m.name, m.desc, "needed", "posts events with subscribers: " +
                            ", ".join("%s (%s)" % (e.rsplit("/", 1)[-1], "/".join(v) if isinstance(v, list) else v) for e, v in sorted(subbed.items())),
                            events=sorted(evs))
                elif not evs and not other and not flow and not value:
                    add("handler", m.name, m.desc, "dead", "handler body has no effect", confidence="high")
                else:
                    why = "other effects: " + ", ".join(sorted(set(other))[:4]) if other else ("changes vanilla flow/value" if flow or value else "")
                    if subbed:
                        why += (" ; " if why else "") + "posts subscribed events"
                    add("handler", m.name, m.desc, "needed", why or "has effects", events=sorted(evs))
            elif anns & ACCESS_ANN:
                owners = {mn} | self.M.descendants(mn) | self.M.ancestors(mn)
                users = {x for o in owners for x in self.callers_exact.get((o, m.name, m.desc), ()) if x in live_other and x in self.live_callers.get((m.name, m.desc), ())}
                jar = {l for o in owners for l in self.jar_mentions.get(o, ())}
                if users or jar:
                    add("accessor", m.name, m.desc, "needed", "called from " + ", ".join(sorted(x.rsplit("/", 1)[-1] for x in users)[:3]) +
                        (" and jars " + ",".join(sorted(jar)) if jar else ""))
                else:
                    add("accessor", m.name, m.desc, "dead", "no caller in live code or the four jars", confidence="high")
            elif not shadow and not (anns & OTHER_ANN - {"Unique", "Intrinsic"}) and not m.name.startswith(("<", "lambda$")) \
                    and not (m.access & (0x0002 | 0x1000 | ACC_ABSTRACT)) and not c.is_interface:
                jar, internal = self.used_sig(m.name, m.desc, live_other)
                shadowed = any(m.name == sm.name and m.desc == sm.desc for o in live_other if o in self.mixins and self.mixins[o]["origin"] == "main"
                               for sm in self.cls[o].methods if any(ann_name(t) == "Shadow" for t, _ in sm.annots))
                if jar or internal or shadowed:
                    add("method", m.name, m.desc, "needed", "used by " + (",".join(sorted(jar)) + " " if jar else "") +
                        (internal[0].rsplit("/", 1)[-1] if internal else "shadowed by another mixin" if shadowed else ""))
                else:
                    add("method", m.name, m.desc, "dead", "added method nobody calls or overrides", confidence="high")
        if not c.is_interface:
            for i in c.interfaces:
                if i not in self.cls:
                    continue
                excl = ifaces
                im = self.iface_methods(i)
                hit, why = None, None
                for ic, imm in im:
                    jar, internal = self.used_sig(imm.name, imm.desc, {x for x in live_other if x not in excl})
                    if jar or internal:
                        hit, why = imm, (",".join(sorted(jar)) or internal[0].rsplit("/", 1)[-1])
                        break
                if hit is None:
                    ment = {x for x in live_other if x not in excl and i in self.cls[x].refs} | self.jar_mentions.get(i, set())
                    if ment:
                        hit, why = "mention", ",".join(sorted(x.rsplit("/", 1)[-1] for x in ment)[:3])
                if hit is not None:
                    add("interface", i, "", "needed", "%s used by %s" % ("type" if hit == "mention" else hit.name, why))
                else:
                    add("interface", i, "", "dead", "no method of it is called/overridden and the type is never referenced", confidence="high")
        # mixin classes that are interfaces (accessor-style) with only Shadow/Accessor members are handled above
        if mn in self.named_by_string:
            add("string", "", "", "uncertain", "class name appears as a string constant (reflection/plugin possible)")
        verdicts = {m["verdict"] for m in members}
        if "needed" in verdicts:
            v = "partly dead" if "dead" in verdicts else "needed"
        elif "uncertain" in verdicts:
            v = "uncertain"
        else:
            v = "dead"
        reason = ""
        if not members:
            reason = "no behavior members (shadow/final fields only)"
        elif v == "dead":
            reason = "; ".join(sorted({m["reason"] for m in members})[:3])
        elif v == "partly dead":
            reason = "dead: " + ", ".join(m["name"] or m["kind"] for m in members if m["verdict"] == "dead")[:120]
        else:
            reason = "; ".join(sorted({m["reason"] for m in members if m["verdict"] != "dead"})[:2])
        return {"verdict": v, "reason": reason, "members": members}

    def solve(self, log=log):
        """Greatest fixpoint: start with every mixin alive, remove those whose members are all unused, until stable."""
        self._ifaces = self.ext_ifaces()
        alive = set(self.mixins)
        verdicts = None
        for it in range(1, 20):
            live_m, live_c = self.method_reach(alive, verdicts)
            self.live_callers = self.live_callers_of(live_m)
            live = {c for c in live_c if self.origin.get(c, "main") == "main"} | alive
            ev_cache = {}
            new_v = {}
            for mn in sorted(self.mixins):
                if self.mixins[mn]["origin"] != "main":
                    new_v[mn] = {"verdict": "needed", "reason": "compat module (not judged)", "members": []}
                    continue
                new_v[mn] = self.judge_mixin(mn, live, ev_cache)
            new_alive = {m for m, v in new_v.items() if v["verdict"] != "dead"} & alive
            log("fixpoint round %d: alive mixins %d, live methods %d, live classes %d" % (it, len(new_alive), len(live_m), len(live_c)))
            verdicts = new_v
            if new_alive == alive:
                break
            alive = new_alive
        self.alive, self.verdicts, self.live, self.live_m, self.live_c = alive, verdicts, live, live_m, live_c
        self.reach_by = self.reach(alive)
        return self.reach_by

    # ---- section A: classes
    def group_of(self, n):
        p = n.rsplit("/", 1)[0] if "/" in n else n
        if p.startswith("net/neoforged/"):
            p = p[len("net/neoforged/"):]
            segs = p.split("/")
            return "/".join(segs[:2]) if segs[0] in ("neoforge", "fml", "neoforgespi") else "/".join(segs[:2])
        if p.startswith("dev/nytheria/compat/"):
            segs = p.split("/")[2:]
            return "dev/nytheria/compat/" + "/".join(segs[:2] if segs and segs[0] in ("injects", "injections", "mixin") else segs[:1])
        return "/".join(p.split("/")[:2])

    def section_a(self):
        by = self.reach_by
        prefixes_live = {}
        for pre, owners in self.prefix_strings.items():
            lo = sorted(o for o in owners if o in self.live)
            if lo:
                prefixes_live[pre] = lo
        rows = {}
        for n in sorted(self.universe):
            groups = sorted(by.get(n, set()))
            main_groups = [g for g in groups if g not in ("compat", "geckolib")]
            is_mixin = n in self.mixins
            if main_groups:
                status = "reachable"
            elif is_mixin and n in self.verdicts and self.verdicts[n]["verdict"] != "dead":
                status = "reachable"
                main_groups = ["mixins"]
            else:
                status = "unreachable"
            note = []
            if status == "unreachable":
                if "compat" in groups:
                    note.append("only compat modules")
                if "geckolib" in groups:
                    note.append("only GeckoLib")
                for pre, lo in prefixes_live.items():
                    if n.startswith(pre):
                        status = "uncertain"
                        note.append("package prefix string '%s' in %s" % (pre, lo[0].rsplit("/", 1)[-1]))
                        break
                if is_mixin:
                    note.append("dead mixin: " + self.verdicts[n]["reason"])
            elif main_groups == ["services"]:
                note.append("only via META-INF/services")
            elif main_groups == ["entry"]:
                note.append("only via fabric.mod.json/plugin/transformer entry")
            rows[n] = {"status": status, "by": main_groups, "loc": self.loc.of(n), "note": "; ".join(note), "mixin": is_mixin}
        return rows

    def section_a2(self):
        """Method-level view: classes with no live method/mention, and dead methods inside live classes."""
        rows = {}
        for n in sorted(self.universe):
            c = self.cls[n]
            live = n in self.live_c or (n in self.alive)
            dead = []
            loc = 0
            if live:
                for m in c.methods:
                    if (m.access & ACC_ABSTRACT) or (n, m.name, m.desc) in self.live_m:
                        continue
                    dead.append(m.name + m.desc)
                    loc += (m.lines[1] - m.lines[0] + 1) if m.lines else 0
            rows[n] = {"live": live, "dead_methods": dead, "dead_method_lines": loc, "loc": self.loc.of(n), "mixin": n in self.mixins}
        return rows

    # ---- section B: events
    def section_b(self):
        live = self.live
        evs = {}
        for e in self.events:
            s = self.subscribers_of(e, live)
            posts = sorted(set(self.post_sites.get(e, [])))
            evs[e] = {"abstract": bool(self.cls[e].access & ACC_ABSTRACT), "subscribers_jars": s["jars"],
                      "subscribers_internal": [list(x) for x in s["internal"]], "post_sites": [list(p) for p in posts],
                      "in_universe": e in self.universe, "loc": self.loc.of(e)}
        for e, v in evs.items():
            v["subscribed"] = bool(v["subscribers_jars"] or v["subscribers_internal"])
            v["posted"] = bool(v["post_sites"])
        # hooks: methods that post; they exist only to post no-subscriber events when every event they mention is unsubscribed
        hooks = []
        for (cn, mn, desc), es in sorted(self.post_methods.items()):
            es = {x for x in es if x in evs}
            if not es:
                continue
            if all(not evs[x]["subscribed"] for x in es):
                m = self.M.method(cn, mn, desc)
                eff_e, other, flow = self.effects(cn, m)
                callers = sorted(x for x in self.callers_exact.get((cn, mn, desc), ()) if x in live and x != cn and x in self.live_callers.get((mn, desc), ()))
                hooks.append({"class": cn, "method": mn, "desc": desc, "events": sorted(es), "post_only": not other, "other": sorted(set(other))[:4],
                              "callers": callers, "live_method": (cn, mn, desc) in self.live_m})
        return evs, hooks

    # ---- section D: extension interface methods
    def section_d(self):
        ifaces, impl = self._ifaces
        live_main = {n for n in self.live if self.origin.get(n, "main") == "main"}
        sigs = {}
        for i in sorted(ifaces):
            for ic, m in self.iface_methods(i):
                if ic == i:
                    sigs[(i, m.name, m.desc)] = m
        wanted = {(n, d) for (_i, n, d) in sigs}
        callers_m = collections.defaultdict(set)  # (name, desc) -> {(class, method)} in live code
        for (cn, mname, mdesc) in self.live_m:
            if cn not in live_main:
                continue
            for k, o, n, d in self.M.method(cn, mname, mdesc).calls:
                if k != "d" and (n, d) in wanted:
                    callers_m[(n, d)].add((cn, mname, mdesc))
        strings = set()
        for cn in live_main:
            strings |= self.cls[cn].strings
        for jc in self.jars.values():
            for c in jc.values():
                strings |= c.strings
        used_jar = {}
        for (i, n, d) in sigs:
            called = self.jar_sigs.get((n, d), set())
            over = self.jar_decl.get((n, d), set())
            used_jar[(i, n, d)] = (sorted(called), sorted(over))
        # fixpoint: an interface method called only from extension-interface methods counts as internal only when that caller is used
        status = {}
        for key in sigs:
            status[key] = None
        def external_callers(key):
            _i, n, d = key
            return [c for c in callers_m.get((n, d), ()) if c[0] not in ifaces]
        for key in sigs:
            ca, ov = used_jar[key]
            if ca or ov:
                status[key] = "jars"
        changed = True
        used_keys = {k for k, v in status.items() if v}
        while changed:
            changed = False
            for key in sigs:
                if key in used_keys:
                    continue
                _i, n, d = key
                ok = bool(external_callers(key))
                if not ok:
                    for (cn, mn, md) in callers_m.get((n, d), ()):
                        if cn in ifaces and any(k2 in used_keys for k2 in sigs if k2[0] == cn and k2[1] == mn and k2[2] == md):
                            ok = True
                            break
                if ok:
                    used_keys.add(key)
                    status[key] = "internal"
                    changed = True
        out = []
        for key, m in sorted(sigs.items()):
            i, n, d = key
            ca, ov = used_jar[key]
            st = status[key] or "unused"
            if st == "unused" and len(n) >= 6 and n in strings:
                st = "uncertain"
            ext = external_callers(key)
            out.append({"interface": i, "method": n, "desc": d, "static": bool(m.access & ACC_STATIC), "default": not (m.access & ACC_ABSTRACT),
                        "status": st, "jars_call": ca, "jars_override": ov,
                        "internal_callers": sorted({c[0] for c in ext})[:5], "internal_caller_count": len({c[0] for c in ext}),
                        "implemented_by_mixin": i in impl})
        return out


# ----------------------------------------------------------------------------------------------------------- output

def short(n):
    return n.rsplit("/", 1)[-1]


def table(head, rows):
    out = ["| " + " | ".join(head) + " |", "|" + "|".join("---" for _ in head) + "|"]
    for r in rows:
        out.append("| " + " | ".join(str(x) for x in r) + " |")
    return out


def reconcile(an, needs_path):
    """Compare with N1 section 13: total / direct / reach, and the direct class set against needs.json."""
    nf = {n for n in an.names if n.startswith(("net/neoforged/", "cpw/"))}
    direct = set()
    for label, jc in an.jars.items():
        if label == "gecko":
            continue
        for c in jc.values():
            direct |= {r for r in c.refs if r in nf}
    top = lambda s: {x for x in s if "$" not in x}  # noqa: E731
    java_nf = {n for n in nf if "/kotlin/" not in (an.cls[n].path or "").replace("\\", "/")}
    res = {"java_only_neoforge_fml_cpw_classes": len(java_nf), "kotlin_neoforge_fml_cpw_classes": len(nf) - len(java_nf),
           "neoforge_fml_cpw_classes": len(nf), "neoforge_fml_cpw_top_level": len(top(nf)),
           "net_neoforged_classes": sum(1 for n in nf if n.startswith("net/neoforged/")),
           "direct_by_jars_all": len(direct), "direct_by_jars_top_level": len(top(direct)),
           "n1_total": 1752, "n1_direct": 262}
    # class-level closure with every mixin alive and no compat/entry roots = N1's 'reach' number
    closure_all = an.closure(direct, set(an.mixins))
    res["closure_from_direct_all"] = len(closure_all & nf)
    res["closure_from_direct_top_level"] = len(top(closure_all & nf))
    if needs_path and os.path.isfile(needs_path):
        nd = read_json(needs_path)
        n1 = set(nd["merged"]["classes"])
        res["n1_merged_classes"] = len(n1)
        res["n1_direct_not_in_tool"] = sorted(n1 - direct)[:40]
        res["tool_direct_not_in_n1"] = sorted(direct - n1)[:40]
        res["n1_direct_not_in_tool_count"] = len(n1 - direct)
        res["tool_direct_not_in_n1_count"] = len(direct - n1)
        res["n1_classes_not_in_nytheria_compat_tree"] = sorted(c for c in n1 if c not in an.names)[:40]
    return res


def build_report(an, repo, needs_path, jar_labels):
    A = an.section_a()
    A2 = an.section_a2()
    evs, hooks = an.section_b()
    D = an.section_d()
    rec = reconcile(an, needs_path)
    tot = collections.Counter(v["status"] for v in A.values())
    loc_by = collections.Counter()
    for n, v in A.items():
        loc_by[v["status"]] += v["loc"]
    data = {
        "meta": {"repo": os.path.basename(repo), "classes_loaded": len(an.cls), "jars": {k: len(v) for k, v in an.jars.items()},
                 "mixins": len(an.mixins), "events": len(an.events)},
        "A": {"counts": dict(tot), "loc": dict(loc_by), "classes": A},
        "A2": {"classes": A2},
        "B": {"events": evs, "hooks_posting_only_unsubscribed_events": hooks,
              "universal_internal_subscribers": an.universal_internal,
              "post_primitives": sorted("%s.%s" % (o, n) for o, n in POST_NAMES)},
        "C": {"mixins": {n: {"targets": an.mixins[n]["targets"], "config": an.mixins[n]["config"], "origin": an.mixins[n]["origin"],
                             **an.verdicts[n]} for n in sorted(an.mixins)},
              "listed_in_config_but_no_class_found": an.mixin_listed_missing},
        "D": D,
        "reconciliation": rec,
        "reflection_hints": {"class_names_spelled_by_strings": sorted(an.named_by_string & an.universe)[:200],
                             "package_prefix_strings": {k: sorted(v)[:10] for k, v in sorted(an.prefix_strings.items())}},
    }
    return data


def render_md(an, data, jar_labels):
    A, B, C, D, R = data["A"], data["B"], data["C"], data["D"], data["reconciliation"]
    md = ["# Nytheria Compat reachability report", "",
          "Generated by `tools/trim/reachability.py` from %d compiled classes, %d mixins, %d events and the jars %s." %
          (data["meta"]["classes_loaded"], data["meta"]["mixins"], data["meta"]["events"], ", ".join(jar_labels)),
          "Full data: `reach.json`. `uncertain` = a string/prefix/reflection path could reach it; such items are never counted as dead.", ""]
    c = A["counts"]
    md += ["## Totals", ""]
    md += table(["", "classes", "LOC"], [["universe (net.neoforged, cpw, dev.nytheria.compat, main jar)", sum(c.values()), sum(A["loc"].values())],
                                          ["reachable", c.get("reachable", 0), A["loc"].get("reachable", 0)],
                                          ["**unreachable**", c.get("unreachable", 0), A["loc"].get("unreachable", 0)],
                                          ["uncertain", c.get("uncertain", 0), A["loc"].get("uncertain", 0)]])
    md += ["", "## A. Classes not reachable from the roots", ""]
    pk = collections.defaultdict(lambda: collections.Counter())
    for n, v in A["classes"].items():
        g = an.group_of(n)
        pk[g]["total"] += 1
        pk[g][v["status"]] += 1
        pk[g]["loc_" + v["status"]] += v["loc"]
        if "compat" in v["note"] or "GeckoLib" in v["note"]:
            pk[g]["other"] += 1
    rows = []
    for g in sorted(pk):
        x = pk[g]
        if x["unreachable"] or x["uncertain"]:
            rows.append([g, x["total"], x["reachable"], x["unreachable"], x["uncertain"], x["loc_unreachable"], x["loc_uncertain"]])
    md += table(["package", "total", "reachable", "unreachable", "uncertain", "LOC unreachable", "LOC uncertain"], rows)
    full = [g for g in sorted(pk) if pk[g]["total"] and pk[g]["reachable"] == 0]
    md += ["", "Packages with no reachable class at all: " + (", ".join("`%s`" % g for g in full) or "none") + ".", ""]
    only_service = sum(1 for v in A["classes"].values() if v["note"].startswith("only via META-INF/services"))
    only_entry = sum(1 for v in A["classes"].values() if v["note"].startswith("only via fabric.mod.json"))
    md += ["Reachable but only through a weak root: %d classes only via `META-INF/services`, %d only via fabric.mod.json/plugin/transformer entries "
           "(see `reach.json` A.classes[].by/note)." % (only_service, only_entry), ""]

    md += ["### A2. Method-level view (stronger than A)", "",
           "Class-level reachability (A) cannot prune hub classes such as `EventHooks`: they mention every event. A2 follows calls (class-hierarchy "
           "analysis) from the same roots, but starts from the *live members* of the mixins only, so dead handlers and the hooks they alone call drop out.", ""]
    A2 = data["A2"]["classes"]
    dead_c = [n for n, v in A2.items() if not v["live"]]
    dm = [(n, v) for n, v in A2.items() if v["live"] and v["dead_methods"]]
    md += table(["", "count", "LOC"], [["classes with no live method or mention", len(dead_c), sum(A2[n]["loc"] for n in dead_c)],
                                       ["... of them mixin classes", sum(1 for n in dead_c if A2[n]["mixin"]), sum(A2[n]["loc"] for n in dead_c if A2[n]["mixin"])],
                                       ["dead methods inside live classes", sum(len(v["dead_methods"]) for _, v in dm), sum(v["dead_method_lines"] for _, v in dm)]])
    pk2 = collections.defaultdict(lambda: collections.Counter())
    for n, v in A2.items():
        g = an.group_of(n)
        pk2[g]["total"] += 1
        if not v["live"]:
            pk2[g]["dead_c"] += 1
            pk2[g]["dead_c_loc"] += v["loc"]
        else:
            pk2[g]["dead_m"] += len(v["dead_methods"])
            pk2[g]["dead_m_loc"] += v["dead_method_lines"]
    md += [""]
    md += table(["package", "classes", "dead classes", "their LOC", "dead methods in live classes", "their lines"],
                [[g, x["total"], x["dead_c"], x["dead_c_loc"], x["dead_m"], x["dead_m_loc"]] for g, x in sorted(pk2.items()) if x["dead_c"] or x["dead_m"]])
    md += [""]

    md += ["## B. Events", ""]
    ev = B["events"]
    n_ev = len(ev)
    unsub = [e for e, v in ev.items() if not v["subscribed"]]
    unposted = [e for e, v in ev.items() if not v["posted"]]
    unsub_posted = [e for e in unsub if ev[e]["posted"]]
    sub_unposted = [e for e, v in ev.items() if v["subscribed"] and not v["posted"]]
    md += table(["", "events"], [["event classes (extend `bus.api.Event`)", n_ev], ["with a subscriber (jars or live Nytheria Compat code)", n_ev - len(unsub)],
                                 ["**no subscriber**", len(unsub)], ["no subscriber and posted somewhere", len(unsub_posted)],
                                 ["no subscriber and never posted", len(unsub) - len(unsub_posted)],
                                 ["subscribed but no post site found (possible functional gap)", len(sub_unposted)]])
    pe = collections.defaultdict(lambda: collections.Counter())
    for e in unsub:
        g = an.group_of(e)
        pe[g]["unsub"] += 1
        pe[g]["posted"] += 1 if ev[e]["posted"] else 0
        pe[g]["loc"] += ev[e]["loc"]
    md += ["", "No-subscriber events by package:", ""]
    md += table(["package", "no subscriber", "of them posted", "LOC"], [[g, x["unsub"], x["posted"], x["loc"]] for g, x in sorted(pe.items())])
    hk = B["hooks_posting_only_unsubscribed_events"]
    po = [h for h in hk if h["post_only"]]
    md += ["", "Hooks (methods with a post site) whose events all have no subscriber: %d; %d of them only post (no other effect found). "
           "%d are still live after dropping the dead mixin members (they have a live caller, or are called from the dead-handler-free code); "
           "%d are not live at all and can go with the dead handlers. Full list in `reach.json` B.hooks_posting_only_unsubscribed_events." %
           (len(hk), len(po), sum(1 for h in hk if h["live_method"]), sum(1 for h in hk if not h["live_method"])), ""]
    if B["universal_internal_subscribers"]:
        md += ["Universal (`Event`) subscribers in Nytheria Compat, ignored for the subscriber test because they are generic registrars: " +
               ", ".join("%s.%s" % (short(a), b) for a, b, _ in B["universal_internal_subscribers"]), ""]
    if sub_unposted:
        md += ["Subscribed by the jars but no post site found: " + ", ".join(short(e) for e in sorted(sub_unposted)[:40]) +
               (" ..." if len(sub_unposted) > 40 else ""), ""]

    md += ["## C. Injects (mixins)", ""]
    cm = C["mixins"]
    vc = collections.Counter(v["verdict"] for v in cm.values())
    md += table(["verdict", "mixins"], [[k, vc.get(k, 0)] for k in ("dead", "partly dead", "uncertain", "needed")])
    pm = collections.defaultdict(lambda: collections.Counter())
    for n, v in cm.items():
        pm[an.group_of(n)][v["verdict"]] += 1
    md += ["", "By package (only packages with a dead / partly dead / uncertain mixin):", ""]
    md += table(["package", "dead", "partly dead", "uncertain", "needed"],
                [[g, x["dead"], x["partly dead"], x["uncertain"], x["needed"]] for g, x in sorted(pm.items()) if x["dead"] or x["partly dead"] or x["uncertain"]])
    for verdict in ("dead", "partly dead", "uncertain"):
        items = [(n, v) for n, v in cm.items() if v["verdict"] == verdict]
        if not items:
            continue
        md += ["", "### %s mixins (%d)" % (verdict, len(items)), ""]
        md += table(["mixin", "target", "reason"], [[short(n).replace("$", "\\$"), ",".join(short(t) for t in v["targets"])[:40], v["reason"][:110].replace("|", "/")]
                                                     for n, v in items[:150]])
        if len(items) > 150:
            md.append("... %d more in reach.json" % (len(items) - 150))

    md += ["", "## D. Extension / injected interface methods", ""]
    st = collections.Counter(d["status"] for d in D)
    md += table(["status", "methods"], [[k, st.get(k, 0)] for k in ("jars", "internal", "unused", "uncertain")])
    pi = collections.defaultdict(lambda: collections.Counter())
    for d in D:
        pi[d["interface"]][d["status"]] += 1
    md += ["", "Per interface (only interfaces with at least one unused method):", ""]
    md += table(["interface", "methods", "jars", "internal", "unused", "uncertain"],
                [[short(i), sum(x.values()), x["jars"], x["internal"], x["unused"], x["uncertain"]] for i, x in sorted(pi.items()) if x["unused"] or x["uncertain"]])

    md += ["", "## Reconciliation with N1 section 13", ""]
    md += table(["figure", "N1", "this tool"], [
        ["NeoForge/FML/cpw classes in the Nytheria Compat build", R["n1_total"], "%d (%d top-level; %d under net/neoforged)" % (R["neoforge_fml_cpw_classes"], R["neoforge_fml_cpw_top_level"], R["net_neoforged_classes"])],
        ["referenced directly by the four jars", R["n1_direct"], "%d (%d top-level)" % (R["direct_by_jars_all"], R["direct_by_jars_top_level"])],
        ["class-level closure from the direct set, all mixins kept", "1200 / 1338 with own refs", "%d (%d top-level)" % (R["closure_from_direct_all"], R["closure_from_direct_top_level"])]])
    if "n1_merged_classes" in R:
        md += ["", "N1 needs.json `merged.classes` lists %d classes; %d are not in the tool's direct set, %d tool-direct classes are not in N1's list (first entries in reach.json)." %
               (R["n1_merged_classes"], R["n1_direct_not_in_tool_count"], R["tool_direct_not_in_n1_count"])]
    md += ["", "## Method and limits", "",
           "- Roots: every class the four jars reference (constant pool, descriptors, signatures, annotations, invokedynamic handles), class names spelled as strings in the jars and in reachable Nytheria Compat code, "
           "fabric.mod.json entrypoints, mixin plugins, class transformers, `META-INF/services` providers, and the alive mixins (section C). GeckoLib and the compat modules are reported separately and are not roots.",
           "- Reachability is class-level (any mention is an edge), so it over-approximates; hub classes pull in a lot. Mixin classes are alive only if their verdict is not `dead`; references to a dead mixin do not revive it.",
           "- Event subscribers: `@SubscribeEvent` parameter types and event types found next to `addListener`/`register` calls (class constants, lambda/method-ref parameters). A subscription to a superclass counts for all subclasses.",
           "- Post sites: methods calling `IEventBus.post*` or `ModLoader.postEvent*`; the posted events are the event classes mentioned in that method (and its lambdas), which can only over-count.",
           "- Handler verdict `dead` needs: no effect found except posting events nobody subscribes to (event ctor/getters, `post`, Ref/CallbackInfo, vanilla getters), or an empty body. `confidence: medium` means a result is applied back (Ref/cancel/value-replacing injector) and is assumed to be conditional on the event outcome; review these.",
           "- Added-member and interface use is matched by name+descriptor (owner-agnostic), which over-counts use; `unused` therefore is safe, `used` may be a name collision.",
           "- Not covered: calls through reflection by computed names, data-driven class lookups, behaviour changes of dead handlers that are not visible as calls (e.g. field writes through accessors are counted as effects, bytecode generated at runtime is not seen)."]
    return "\n".join(md) + "\n"


JAR_FILES = [("cataclysm", "L_Ender's Cataclysm 1.21.1-3.33.jar"), ("astral", "astral_dimension-neoforge-1.21.1-3.0.jar"),
             ("curios", "curios-neoforge-9.5.1+1.21.1.jar"), ("lionfish", "lionfishapi-3.1.jar"), ("gecko", "geckolib-neoforge-1.21.1-4.9.3.jar")]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo", default=os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
    ap.add_argument("--classes", action="append", help="class directory (repeatable); default: build/classes of root, loader/*, compat/*")
    ap.add_argument("--originals", required=True, help="folder with the target mod jars")
    ap.add_argument("--needs")
    ap.add_argument("--out", required=True)
    ap.add_argument("--cache")
    a = ap.parse_args()
    repo = a.repo
    if a.cache:
        os.makedirs(a.cache, exist_ok=True)
    dirs = [("main", d) for d in a.classes] if a.classes else default_class_dirs(repo)
    if not dirs:
        sys.exit("no build/classes found under %s; run ./gradlew remapJar first" % repo)
    cls, origin = {}, {}
    for label, d in dirs:
        part = load_dir(d, a.cache)
        log("loaded %5d classes from %s (%s)" % (len(part), d, label))
        for n, c in part.items():
            cls[n] = c
            origin[n] = label
    jars, labels = {}, []
    for label, fn in JAR_FILES:
        p = os.path.join(a.originals, fn)
        if os.path.isfile(p):
            jars[label] = load_jar(p, a.cache)
            labels.append(label)
            log("loaded %5d classes from %s" % (len(jars[label]), fn))
        elif label != "gecko":
            sys.exit("missing target jar " + p)
    res = read_resource_roots(repo)
    loc = Loc(repo, cls)
    log("source files matched: %d" % loc.files_found)
    an = Analysis(repo, cls, origin, jars, res, loc)
    an.solve()
    data = build_report(an, repo, a.needs, labels)
    os.makedirs(a.out, exist_ok=True)
    with open(os.path.join(a.out, "reach.json"), "w", encoding="utf-8") as f:
        json.dump(data, f, indent=1, sort_keys=True)
    with open(os.path.join(a.out, "reach-report.md"), "w", encoding="utf-8") as f:
        f.write(render_md(an, data, labels))
    log("wrote %s" % a.out)


if __name__ == "__main__":
    main()
