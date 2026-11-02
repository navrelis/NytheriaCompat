#!/usr/bin/env python3
"""Nytheria Compat trim: one verdict per injector, from bytecode (dev only, Python 3 stdlib, not part of the jar).

Usage (from the repo root, after `./gradlew classes`):

    py -3 tools/trim/hook-reach.py --mods <mods-folder>

<mods-folder> holds the four mod jars (Cataclysm, Astral Dimension, Lionfish API, Curios).

Writes tools/trim/hook-need.tsv. HookRulesTest fails on an injector with no row and on a row with no injector.
Inputs : build/classes (injects, mixin, injections, hook, forge), the four mod jars, the Minecraft jars, nytheria_compat.classtweaker.
Rule   : REMOVE only with proof that no mod and no forge/ code reaches the handler. What this tool cannot rule out is a
         blocker, and a blocker makes the row KEEP. Hand-checked rows are in hook-need-manual.tsv and win over the tool;
         rows of kind `note` there are findings that are not injectors.

For every handler the walk follows its own bytecode, the mixin's helpers, lambdas, hook/ and helpers/, NeoForge code and
extension method defaults, and collects four things: events created, extension methods called (a method NeoForge adds to
a vanilla class), own fields touched, and everything it cannot explain. Then:
  - an event with a listener, or an extension method a mod or forge/ overrides, is a need: KEEP, or NARROW if Scope or
    NeoFluids can test the receiver of that method;
  - a handler that only writes state stays as long as something kept reads that state (fixpoint over all handlers);
  - no need and no blocker and at least one thing proven dead is REMOVE. A handler with nothing to prove stays `unchecked`.
"""
import argparse
import collections
import csv
import glob
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import importlib  # noqa: E402

reachability = importlib.import_module("reachability")
load_dir, load_jar = reachability.load_dir, reachability.load_jar

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODS = None  # set from --mods in main()
JARS = {"cataclysm": "L_Ender's Cataclysm*.jar", "astral": "astral_dimension*.jar", "lionfish": "lionfishapi*.jar", "curios": "curios*.jar"}
MC_JARS = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-%s/1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2/*.jar")
OUT = os.path.join(ROOT, "tools/trim/hook-need.tsv")
MANUAL = os.path.join(ROOT, "tools/trim/hook-need-manual.tsv")
COLUMNS = ["file", "handler", "target method", "kind", "area", "verdict", "needed by", "condition", "proof"]

MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;"
SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;"
EVENT = "net/neoforged/bus/api/Event"
SUBSCRIBE = "Lnet/neoforged/bus/api/SubscribeEvent;"
ACCESSORS = ("Lorg/spongepowered/asm/mixin/gen/Accessor;", "Lorg/spongepowered/asm/mixin/gen/Invoker;")
INJECTORS = {"L" + p + ";" for p in (
    "org/spongepowered/asm/mixin/Overwrite", "org/spongepowered/asm/mixin/injection/Inject", "org/spongepowered/asm/mixin/injection/Redirect",
    "org/spongepowered/asm/mixin/injection/ModifyArg", "org/spongepowered/asm/mixin/injection/ModifyArgs",
    "org/spongepowered/asm/mixin/injection/ModifyVariable", "org/spongepowered/asm/mixin/injection/ModifyConstant",
    "com/llamalad7/mixinextras/injector/ModifyExpressionValue", "com/llamalad7/mixinextras/injector/ModifyReturnValue",
    "com/llamalad7/mixinextras/injector/ModifyReceiver", "com/llamalad7/mixinextras/injector/WrapWithCondition",
    "com/llamalad7/mixinextras/injector/v2/WrapWithCondition", "com/llamalad7/mixinextras/injector/wrapoperation/WrapOperation",
    "com/llamalad7/mixinextras/injector/wrapmethod/WrapMethod")}

# What needs no walk: tests that decide nothing by themselves, and libraries.
SCOPED = ("dev/nytheria/compat/hook/Scope", "dev/nytheria/compat/hook/NeoFluids", "dev/nytheria/compat/util/NytheriaCompatHelper$MethodOverrideCheck")
NEUTRAL_NAMES = {"createMixinException"}
LIBRARIES = ("java/", "kotlin/", "org/spongepowered/", "com/llamalad7/", "org/slf4j/", "org/apache/", "it/unimi/", "com/google/", "com/mojang/serialization/",
             "com/mojang/datafixers/", "com/mojang/logging/", "com/mojang/brigadier/", "org/joml/")
REFLECTION = {("java/lang/Class", n) for n in ("forName", "getMethod", "getDeclaredMethod", "getField", "getDeclaredField")} | {("java/lang/reflect/Method", "invoke"), ("java/lang/reflect/Field", "get"), ("java/lang/reflect/Field", "set")}
# NeoForge classes that are plain values or the ways into the walk, so touching them proves nothing and blocks nothing.
PLAIN_NEO = {"net/neoforged/neoforge/common/util/TriState", "net/neoforged/neoforge/common/ItemAbility", "net/neoforged/neoforge/common/ItemAbilities",
             "net/neoforged/neoforge/common/CommonHooks", "net/neoforged/neoforge/event/EventHooks", "net/neoforged/neoforge/client/ClientHooks",
             "net/neoforged/neoforge/common/NeoForge"}
PLAIN_NEO_PACKAGES = ("net/neoforged/bus/", "net/neoforged/neoforge/common/extensions/", "net/neoforged/neoforge/client/extensions/",
                      "net/neoforged/neoforge/event/", "net/neoforged/neoforge/client/event/")
NEO_ATTRIBUTES = {"SWIM_SPEED", "NAMETAG_DISTANCE", "CREATIVE_FLIGHT"}
MUTATORS = ("set", "add", "remove", "put", "clear", "hurt", "kill", "discard", "spawn", "send", "broadcast", "drop", "destroy", "place", "play",
            "push", "move", "teleport", "shrink", "grow", "insert", "extract", "save", "load", "register", "reset", "update", "tick", "mark")
VALUES = {"Vec3", "AABB", "BlockPos", "BlockPos$MutableBlockPos", "BlockState", "FluidState", "ChunkPos", "ProfilerFiller", "PoseStack", "MutableComponent", "Component", "ResourceLocation", "CrashReportCategory"}
HOLDERS = ("Ljava/lang/ThreadLocal;", "Ljava/util/concurrent/atomic/", "Ljava/util/List;", "Ljava/util/Map;", "Ljava/util/Set;", "Ljava/util/Deque;", "Lit/unimi/")

# Receiver of an extension method -> the test that tells a mod's object. Keyed by the class the method is declared on.
BI_PREDICATE = "(Ljava/util/function/BiPredicate;)Z"
SCOPE_TESTS = [
    (("IBlockExtension", "IBlockStateExtension", "world/level/block/"), "Scope.isNeoObject(state.getBlock())"),
    (("IItemExtension", "IItemStackExtension", "world/item/"), "Scope.isNeoObject(stack.getItem())"),
    (("IFluidExtension", "IFluidStateExtension", "world/level/material/"), "NeoFluids.isNeo(fluidState)"),
    (("IEntityExtension", "ILivingEntityExtension", "IPlayerExtension", "world/entity/"), "Scope.isNeoObject(entity)"),
]


class Facts:
    def __init__(self):
        self.events, self.ext, self.other, self.mutates, self.reads, self.writes = set(), {}, set(), set(), set(), set()
        self.attributes = set()
        self.scoped = self.reflection = False
        self.seen = set()


def plain_neo(owner):
    return owner.split("$")[0] in PLAIN_NEO or owner.startswith(PLAIN_NEO_PACKAGES)


def is_vanilla(name):
    return name.startswith(("net/minecraft/", "com/mojang/")) and not name.startswith(LIBRARIES)


def short(name):
    return name.rsplit("/", 1)[-1]


def ret(desc):
    return desc.split(")", 1)[1]


class Model:
    def __init__(self, cache):
        self.dev = {}
        for sub in ("java", "kotlin"):
            self.dev.update(load_dir(os.path.join(ROOT, "build/classes", sub, "main"), cache))
        self.side = {}             # compat and loader: only their listeners count
        for folder in glob.glob(os.path.join(ROOT, "compat/*/build/classes/*/main")) + glob.glob(os.path.join(ROOT, "loader/**/build/classes/*/main"), recursive=True):
            self.side.update(load_dir(folder, cache))
        self.mc = {}
        for side in ("common", "clientonly"):
            for jar in glob.glob(MC_JARS % side):
                self.mc.update(load_jar(jar, cache))
        self.mods, self.owner_mod = {}, {}
        for mod, pattern in JARS.items():
            for jar in glob.glob(os.path.join(MODS, pattern)):
                classes = load_jar(jar, cache)
                self.mods.update(classes)
                self.owner_mod.update(dict.fromkeys(classes, mod))
        self.all = {**self.mc, **self.dev, **self.mods}
        self.injected = collections.defaultdict(set)
        with open(os.path.join(ROOT, "src/main/resources/nytheria_compat.classtweaker")) as f:
            for line in f:
                parts = line.split()
                if len(parts) == 3 and parts[0].endswith("inject-interface"):
                    self.injected[parts[1]].add(parts[2])
        self.targets = {}          # mixin -> its target classes
        self.mixins_of = collections.defaultdict(list)
        for c in self.dev.values():
            targets = mixin_targets(c)
            if targets:
                self.targets[c.name] = targets
                for t in targets:
                    self.mixins_of[t].append(c.name)
                    self.injected[t].update(c.interfaces)
                    if c.super != "java/lang/Object":
                        self.injected[t].add(c.super)
                for desc, values in c.annots:
                    if desc.endswith("ChangeSuperClass;"):
                        for t in targets:
                            self.injected[t].add(values["value"][1][1:-1] if isinstance(values.get("value"), tuple) else "net/neoforged/neoforge/attachment/AttachmentHolder")
        self.anc = {}
        self.decls = collections.defaultdict(list)     # (name, desc) -> [declaring class outside Minecraft, with its target for a mixin]
        for c in list(self.dev.values()) + list(self.mods.values()):
            for m in c.methods:
                if not m.access & 0x0008 and m.name not in ("<init>", "<clinit>") and not any(t == SHADOW for t, _ in m.annots):
                    self.decls[m.name, m.desc].append(c.name)
        self.called_by_mods = collections.defaultdict(set)
        self.called_by_dev = collections.defaultdict(set)
        for c in self.mods.values():
            for m in c.methods:
                for _, _, n, d in m.calls:
                    self.called_by_mods[n, d].add(c.name + "#" + m.name)
        for c in self.dev.values():
            if c.name not in self.targets:
                for m in c.methods:
                    for _, _, n, d in m.calls:
                        self.called_by_dev[n, d].add(c.name + "#" + m.name)
        self.mod_strings = set()
        self.mod_fields = collections.defaultdict(set)
        for c in self.mods.values():
            self.mod_strings |= c.strings
            for m in c.methods:
                for _, owner, name, _ in m.fields:
                    self.mod_fields[owner, name].add(self.owner_mod[c.name] + " " + short(c.name) + "#" + m.name)
        self.field_readers = collections.defaultdict(list)
        for c in self.dev.values():
            for m in c.methods:
                for kind, owner, name, _ in m.fields:
                    if kind in ("g", "gs") and m.name != "<clinit>":
                        self.field_readers[owner + "." + name].append((c, m))
        self.listeners = self.scan_listeners()
        self.mod_mixins = self.scan_mod_mixins()

    def ancestors(self, name):
        got = self.anc.get(name)
        if got is None:
            got = self.anc[name] = {name}
            c = self.all.get(name)
            parents = set(self.injected.get(name, ()))
            if c is not None:
                parents.update(c.interfaces)
                if c.super:
                    parents.add(c.super)
            for p in parents:
                got |= self.ancestors(p)
        return got

    def is_event(self, name):
        return EVENT in self.ancestors(name)

    def scan_listeners(self):
        out = collections.defaultdict(set)
        for label, classes in (("", self.mods), ("forge", self.dev), ("forge", self.side)):
            for c in classes.values():
                who = label or self.owner_mod[c.name]
                for m in c.methods:
                    if any(t == SUBSCRIBE for t, _ in m.annots):
                        events = reachability.desc_classes(m.desc.split(")")[0])[:1]
                    elif any(n in ("addListener", "addGenericListener") for _, _, n, _ in m.calls):
                        events = {x for x in m.classes if self.is_event(x)}
                        for k, _, _, d in m.calls:
                            if k == "h":
                                events.update(x for x in reachability.desc_classes(d.split(")")[0]) if self.is_event(x))
                    else:
                        continue
                    for e in events:
                        if self.is_event(e):
                            out[e].add(who + " " + short(c.name) + "#" + m.name)
        return out

    def listeners_of(self, event):
        got = set()
        for a in self.ancestors(event):
            got |= self.listeners.get(a, set())
        return got

    def scan_mod_mixins(self):
        """(target class, target method) of every injector in a mod's own mixins."""
        out = collections.defaultdict(set)
        for c in self.mods.values():
            targets = mixin_targets(c)
            for m in c.methods:
                for t, values in m.annots:
                    if t in INJECTORS or t.startswith("Lorg/spongepowered/asm/mixin/injection/"):
                        for spec in as_list(values.get("method")):
                            for target in targets:
                                out[target, spec.split("(")[0]].add(short(c.name) + "#" + m.name)
        return out

    def vanilla_member(self, owner, name, desc):
        for a in self.ancestors(owner):
            c = self.mc.get(a)
            if c is not None and any(m.name == name and m.desc == desc for m in c.methods):
                return True
        return False

    def find(self, owner, name, desc):
        """Bodies a call can run in Nytheria Compat's own classes: the owner's chain, and mixins on any class of it."""
        out = []
        for a in self.ancestors(owner):
            c = self.dev.get(a)
            if c is not None:
                out += [(c, m) for m in c.methods if m.name == name and m.desc == desc]
            for mixin in self.mixins_of.get(a, ()):
                c = self.dev[mixin]
                out += [(c, m) for m in c.methods if m.name == name and m.desc == desc and not any(t == SHADOW for t, _ in m.annots)]
        return out

    def overriders(self, owner, name, desc):
        """Classes outside Minecraft that declare this method and reach `owner`'s declaring class, as (who, class)."""
        out = set()
        defs = {a for a in self.ancestors(owner) if self.declares(a, name, desc)}
        for c in self.decls.get((name, desc), ()):
            hosts = self.targets.get(c, [c])
            if c in defs or not any(defs & self.ancestors(h) for h in hosts):
                continue
            who = self.owner_mod.get(c) or ("nytheria_compat" if c in self.targets else "forge" if c.startswith("net/neoforged/") else "nytheria_compat")
            out.add((who, c))
        return out

    def has_default(self, owner, name, desc):
        """The method has a default body on a NeoForge extension interface, so an unchanged call is vanilla behaviour."""
        for a in self.ancestors(owner):
            c = self.dev.get(a)
            if c is not None and c.is_interface and "/extensions/" in a:
                if any(m.name == name and m.desc == desc and not m.access & 0x0400 for m in c.methods):
                    return True
        return False

    def declares(self, cls, name, desc):
        c = self.all.get(cls)
        if c is not None and any(m.name == name and m.desc == desc for m in c.methods):
            return True
        return any(self.declares(m, name, desc) for m in self.mixins_of.get(cls, ()))


def as_list(v):
    return v if isinstance(v, list) else [] if v is None else [v]


def mixin_targets(c):
    v = c.annot(MIXIN)
    if v is None:
        return []
    out = [x[1][1:-1] for x in as_list(v.get("value")) if isinstance(x, tuple)]
    return out + [x.replace(".", "/") for x in as_list(v.get("targets"))]


def walk(model, cls, method):
    facts = Facts()
    stack = [(cls, method)]
    while stack:
        c, m = stack.pop()
        if (c.name, m.name, m.desc) in facts.seen:
            continue
        facts.seen.add((c.name, m.name, m.desc))
        if any(t in ACCESSORS for t, _ in m.annots):
            (facts.mutates.add("accessor " + short(c.name) + "#" + m.name) if ret(m.desc) == "V" else None)
            continue
        for kind, owner, name, desc in m.calls:
            visit_call(model, facts, stack, c, kind, owner, name, desc)
        for kind, owner, name, desc in m.fields:
            if not (m.name == "<init>" and kind == "p" and owner in model.ancestors(c.name)):   # a new object filling itself
                visit_field(model, facts, c, kind, owner, name, desc)
    return facts


def visit_call(model, facts, stack, c, kind, owner, name, desc):
    if owner.startswith("["):
        return
    if owner in SCOPED or name.startswith("hasMethodOverride"):
        facts.scoped = True
        return
    if name in NEUTRAL_NAMES:
        return
    if owner == "dev/nytheria/compat/hook/EventGate":
        return
    if (owner, name) in REFLECTION:
        facts.reflection = True
    if owner.startswith("net/fabricmc/") or owner.startswith("io/github/fabricators_of_create/"):
        facts.other.add(short(owner))
        return
    if owner.startswith(LIBRARIES):
        return
    if model.is_event(owner):      # an event is a value: its constructor and getters do nothing else
        if name == "<init>":
            facts.events.add(owner)
        return
    if owner.startswith("net/neoforged/bus/") or owner.startswith("net/neoforged/fml/"):
        return
    if is_vanilla(owner) or owner in model.targets:
        host = model.targets.get(owner, [owner])[0]
        if model.vanilla_member(host, name, desc):
            if name != "<init>" and short(host) not in VALUES and (ret(desc) == "V" or name.startswith(MUTATORS)):
                facts.mutates.add(short(host) + "#" + name)
            return
        bodies = model.find(owner, name, desc)
        if not bodies:      # inherited from the JDK
            return
        internal = name.startswith(("nytheriacompat$", "lambda$", "neo$")) or all(m.access & 0x1002 for _, m in bodies)
        if not internal:
            facts.ext.setdefault((name, desc), set()).add(host)
            for who, cls in model.overriders(host, name, desc):
                if who == "nytheria_compat":
                    stack.extend(model.find(cls, name, desc))
    elif owner.startswith("net/neoforged/"):
        if not plain_neo(owner):
            facts.other.add(short(owner))
        elif owner.startswith(("net/neoforged/neoforge/common/extensions/", "net/neoforged/neoforge/client/extensions/")) and name != "self" and kind != "s":
            facts.ext.setdefault((name, desc), set()).add(owner)
    elif owner in model.mods:
        facts.other.add(short(owner))
    for pair in model.find(owner, name, desc):
        stack.append(pair)


def visit_field(model, facts, c, kind, owner, name, desc):
    if is_vanilla(owner):
        if kind in ("p", "ps"):
            facts.mutates.add(short(owner) + "." + name)
        return
    if owner.startswith("net/neoforged/"):
        if kind in ("p", "ps"):
            facts.mutates.add(short(owner) + "." + name)
        elif owner.endswith("/NeoForgeMod") and name in NEO_ATTRIBUTES:
            facts.attributes.add(name)
        elif not plain_neo(owner):
            facts.other.add(short(owner))
        return
    decl = model.dev.get(owner)
    field = next((f for f in decl.fields if f[0] == name), None) if decl else None
    if field and any(t == SHADOW for t, _ in field[3]):
        if kind in ("p", "ps"):
            facts.mutates.add(short(owner) + "." + name)
        return
    state = owner + "." + name
    if kind in ("p", "ps") or desc.startswith(HOLDERS):
        facts.writes.add(state)
    if kind in ("g", "gs"):
        facts.reads.add(state)


class Row:
    def __init__(self, cls, method, annot, values):
        self.cls, self.method, self.annot = cls, method, annot
        self.file = "src/main/java/" + cls.name.split("$")[0] + ".java"
        self.target = target_text(cls, values)
        self.facts = None
        self.needs, self.blockers, self.dead = [], [], []
        self.verdict, self.condition = "KEEP", ""


def target_text(cls, values):
    at = []
    for a in as_list(values.get("at")):
        if isinstance(a, tuple):
            spec = a[1].get("target") or a[1].get("value") or ""
            at.append(short(spec.split(";")[0]) + "." + spec.split(";")[1].split("(")[0] if ";" in spec and "." not in spec.split(";")[1] and spec.split(";")[1] else str(spec))
    methods = ",".join(x.split("(")[0] for x in as_list(values.get("method")))
    return "|".join(short(t) for t in mixin_targets(cls)) + "#" + methods + (" @" + ",".join(at) if at else "")


def area_of(path):
    p = path.replace("src/main/java/dev/nytheria/compat/", "")
    if p.startswith("injects/world/level/"):
        return "block"
    if p.startswith("injects/world/entity/"):
        return "mob"
    if p.startswith(("injects/world/item/", "injects/world/inventory/", "injects/world/effect/", "injects/world/damagesource/")):
        return "item"
    if p.startswith(("injects/client/", "mixin/compat/")):
        return "client"
    return "core" if p.startswith("mixin/") else "server"


def classify(model, row):
    facts = row.facts = walk(model, row.cls, row.method)
    targets = mixin_targets(row.cls)
    if not all(is_vanilla(t) for t in targets):
        row.blockers.append("target is not vanilla")
    for spec in row_methods(row):
        for t in targets:
            hits = model.mod_mixins.get((t, spec))
            if hits:
                row.blockers.append("mod mixin on same method: " + sorted(hits)[0])
            if t in model.mc and spec not in ("<init>", "<clinit>") and not any(m.name == spec for m in model.mc[t].methods):
                row.blockers.append("target method is not vanilla: " + spec)
    if facts.reflection:
        row.blockers.append("reflection")
    if facts.mutates:
        row.blockers.append("changes state: " + ",".join(sorted(facts.mutates)[:3]))
    if facts.other:
        row.blockers.append("touches: " + ",".join(sorted(facts.other)[:4]))
    for event in sorted(facts.events):
        who = model.listeners_of(event)
        if who:
            row.needs.append(short(event) + " <- " + sorted(who)[0] + (" +%d" % (len(who) - 1) if len(who) > 1 else ""))
        else:
            row.dead.append("event " + short(event) + " has no listener")
    tests = set()
    for (name, desc), owners in sorted(facts.ext.items()):
        over = set()
        for owner in owners:
            over |= model.overriders(owner, name, desc)
        strong = sorted(x for x in over if x[0] != "nytheria_compat")
        if strong:
            row.needs.append(name + " <- " + strong[0][0] + " " + short(strong[0][1]) + (" +%d" % (len(strong) - 1) if len(strong) > 1 else ""))
            tests.add(scope_test(sorted(owners)[0]) if all(x[0] != "forge" for x in strong) else None)
        elif not model.has_default(sorted(owners)[0], name, desc):
            row.blockers.append("patch method, its body is the behaviour: " + name)
        else:
            row.dead.append(name + " has no override" + (" (only Nytheria Compat's on vanilla: %s)" % short(sorted(over)[0][1]) if over else ""))
            if name in model.mod_strings:
                row.blockers.append("name in a mod string: " + name)
    for name in sorted(facts.attributes):
        who = model.mod_fields.get(("net/neoforged/neoforge/common/NeoForgeMod", name))
        if who:
            row.needs.append("attribute " + name + " <- " + sorted(who)[0])
        else:
            row.dead.append("attribute " + name + ": no mod reads it")
    if is_fluid(facts, row):
        row.blockers.append("fluid hook")
        row.needs.append("astral fluid types (no override needed)")
    if facts.scoped:
        row.condition = "already scoped"
    elif ("isInFluidType", BI_PREDICATE) in facts.ext and ("isInFluidType", "()Z") not in facts.ext and not facts.mutates:
        row.verdict, row.condition = "NARROW", "this.isInFluidType() before the lambda: the map holds only NeoForge fluids"
    elif row.needs and tests and None not in tests and len(tests) == 1 and not facts.events and not is_fluid(facts, row):
        row.verdict, row.condition = "NARROW", tests.pop()


def row_methods(row):
    return [x.split("(")[0] for x in row.target.split("#", 1)[1].split(" @")[0].split(",") if x]


def scope_test(owner):
    for keys, test in SCOPE_TESTS:
        if any(k in owner for k in keys):
            return test
    return None


def is_fluid(facts, row):
    return any("Fluid" in x for x in facts.other) or any("Fluid" in n for n, _ in facts.ext) or "Fluid" in row.target or "Bucket" in row.target


def settle(model, rows):
    """A row is REMOVE if nothing needs it. Fixpoint: what feeds a kept handler stays."""
    def pure_capture(r):
        return r.facts.writes and r.facts.writes >= r.facts.reads and not r.facts.ext and not r.facts.events and ret(r.method.desc) == "V" and r.annot.endswith("/Inject;")
    candidates = {r for r in rows if not r.needs and not r.blockers and (r.dead or pure_capture(r))}
    kept_read = collections.defaultdict(list)
    def reader_map():
        kept_read.clear()
        for r in rows:
            if r not in candidates:
                for s in r.facts.reads | r.facts.writes:
                    kept_read[s].append(r)
    outside = {}
    for owner_field in {s for r in rows for s in r.facts.writes | r.facts.reads}:
        outside[owner_field] = readers_outside(model, rows, owner_field)
    shared = collections.defaultdict(list)
    for r in rows:
        if "sugar/ref/Local" in r.method.desc:
            shared[r.cls.name, r.target.split(" @")[0]].append(r)
    changed = True
    while changed:
        changed = False
        reader_map()
        for r in list(candidates):
            for s in sorted(r.facts.writes):
                reader = next((k for k in kept_read.get(s, ()) if k is not r), None)
                if reader is not None or outside[s]:
                    who = outside[s] or short(reader.cls.name) + "#" + reader.method.name
                    r.needs.append("feeds " + who + " via " + short(s))
                    candidates.discard(r)
                    changed = True
                    break
        for group in shared.values():
            if len(group) > 1 and any(r not in candidates for r in group) and any(r in candidates for r in group):
                for r in group:
                    if r in candidates:
                        r.needs.append("shares a local with a kept handler")
                        candidates.discard(r)
                changed = True
    # A capture goes only together with a handler that reads what it stores; one with no reader at all is not proof, a read may be elsewhere.
    for r in sorted(candidates, key=lambda r: r.method.name):
        if pure_capture(r) and not any(q is not r and s in q.facts.reads for q in candidates for s in r.facts.writes):
            r.needs.clear()
            candidates.discard(r)
    for r in candidates:
        r.verdict = "REMOVE"


def readers_outside(model, rows, state):
    """A method that reads this state and that no handler reaches: an extension method body or forge/ code. If nothing calls it, it reads nothing."""
    reached = set()
    for r in rows:
        if state in r.facts.reads | r.facts.writes:
            reached |= r.facts.seen
    for c, m in model.field_readers.get(state, ()):
        if (c.name, m.name, m.desc) in reached or m.name.startswith("lambda$") or any(t in INJECTORS for t, _ in m.annots):
            continue
        if model.called_by_mods.get((m.name, m.desc)) or model.called_by_dev.get((m.name, m.desc)) or model.overriders(c.name, m.name, m.desc):
            return short(c.name) + "#" + m.name
    return ""


def describe(row):
    f = row.facts
    kind = "fires event" if f.events else "calls extension method" if f.ext else "capture" if f.writes and not f.mutates else "other"
    if row.verdict == "REMOVE":
        proof = "; ".join(dict.fromkeys(row.dead)) or "stores state only handlers marked REMOVE read"
    elif row.blockers and not row.needs:
        proof = "; ".join(dict.fromkeys(row.blockers))
    elif row.needs:
        proof = "needed"
    else:
        proof = "unchecked"
    return kind, proof


def write(rows, manual, notes):
    rows = sorted(rows, key=lambda r: (r.file, r.method.name))
    counts = collections.Counter()
    with open(OUT, "w", newline="", encoding="utf-8") as f:
        out = csv.writer(f, delimiter="\t", lineterminator="\n", quoting=csv.QUOTE_NONE, quotechar=None)
        out.writerow(COLUMNS)
        for r in rows:
            kind, proof = describe(r)
            line = [r.file, r.method.name, r.target, kind, area_of(r.file), r.verdict, "; ".join(dict.fromkeys(r.needs)), r.condition, proof]
            fix = manual.get((r.file, r.method.name))
            if fix:
                line[5:9] = fix
            out.writerow([x.replace("\t", " ") for x in line])
            counts[line[4], line[5]] += 1
        out.writerows(notes)
    return counts


def read_manual():
    """Rows of six columns fix a handler (file, handler, verdict, needed by, condition, proof); nine are `note` rows, kept as they are."""
    fixes, notes = {}, []
    with open(MANUAL, encoding="utf-8") as f:
        for line in f:
            p = line.rstrip("\n").split("\t")
            if len(p) == 6 and not p[0].startswith("#"):
                fixes[p[0], p[1]] = p[2:]
            elif len(p) == 9:
                notes.append(p)
    return fixes, notes


def main():
    global MODS
    ap = argparse.ArgumentParser(description="One verdict per injector, from bytecode.")
    ap.add_argument("--mods", required=True, help="folder with the four mod jars")
    MODS = ap.parse_args().mods
    model = Model(None)
    rows = []
    for c in model.dev.values():
        if c.name.startswith(("dev/nytheria/compat/injects/", "dev/nytheria/compat/mixin/")):
            for m in c.methods:
                for t, values in m.annots:
                    if t in INJECTORS:
                        rows.append(Row(c, m, t, values))
                        break
    for r in rows:
        classify(model, r)
    settle(model, rows)
    counts = write(rows, *read_manual())
    areas = sorted({a for a, _ in counts})
    for area in areas:
        print(area, {v: counts[area, v] for v in ("KEEP", "NARROW", "REMOVE")})
    print("total", len(rows))


if __name__ == "__main__":
    main()
