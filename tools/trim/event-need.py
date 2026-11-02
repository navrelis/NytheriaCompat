#!/usr/bin/env python3
"""Event need table: which NeoForge event each mod needs, and the narrowest condition under which a listener can act.

    py -3 tools/trim/event-need.py [--jars <dir>] [--out tools/trim/event-need.tsv]

Reads the four mod jars (plus geckolib-neoforge, which Nytheria Compat does not load, as info) and Nytheria Compat's compiled classes
(run `./gradlew classes` first). The listeners and "what the handler tests first" come from the bytecode; a listener of a
parent class counts for its subclasses. An event a Nytheria Compat hook gates with EventGate.has and nobody listens to gets a row
with verdict NONE. Need, effect, condition, proof and verdict come from HAND below, written from reading the handlers with
javap. An event without a HAND row is ALWAYS: it keeps only the EventGate.has check.

Columns: event, side, listeners (mod:class#method), first (what each handler tests first), need (the method of Need), effect,
condition, proof, verdict. HookRulesTest holds this table against the hooks.
"""
import argparse
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from classfile import desc_classes, parse_class  # noqa: E402

JARS = "originals"
LABELS = (("L_Ender", "cataclysm"), ("astral_dimension", "astral"), ("lionfishapi", "lionfish"), ("curios", "curios"), ("geckolib", "geckolib(info)"))
SUBSCRIBE = "Lnet/neoforged/bus/api/SubscribeEvent;"
EVENT = "net/neoforged/bus/api/Event"
EVENT_GATE = "dev/nytheria/compat/hook/EventGate"
NYTHERIACOMPAT_ROOTS = ("build/classes/java/main", "build/classes/kotlin/main")
NYTHERIACOMPAT_PREFIXES = ("dev/nytheria/compat/", "net/neoforged/neoforge/")

# From reading the handlers: event -> (Need method, effect, condition, proof, verdict). Offsets are of the jars in originals/.
# Every effect and item a proof names was checked against the mod's registration: namespace, then path.
HAND = {
    "event.entity.EntityEvent$EntityConstructing": ("entityConstructing", "Curios attaches its inventory", "entity is a LivingEntity",
        "CuriosEventHandler#entityConstructing @6 instanceof LivingEntity, else return", "NARROW"),
    "event.tick.EntityTickEvent$Post": ("entityTickPost", "Curios ticks its slots", "entity is a LivingEntity",
        "CuriosEventHandler#tick @6 instanceof LivingEntity, else return", "NARROW"),
    "event.entity.player.PlayerEvent$StartTracking": ("startTracking", "Curios syncs the tracked mob's slots", "ServerPlayer tracking a LivingEntity",
        "CuriosEventHandler#playerStartTracking @11 instanceof ServerPlayer, @24 instanceof LivingEntity, else return", "NARROW"),
    "event.entity.living.LivingShieldBlockEvent": ("shieldBlock", "Cataclysm parry, shield damage; Astral rose shield buffs", "maledictio_sagitta damage, or a mod item in use or in a hand",
        "onShieldDamage acts @31 on MALEDICTIO_SAGITTA, @58 @173 on getUseItem BULWARK_OF_THE_FLAME or AZURE_SEA_SHIELD; its getData(PARRY_ATTACHMENT) @43 only makes the default, read on those branches. PlayerBlocksWithRoseShieldProcedure#execute acts @36 @70 on ROSE_SHIELD off hand or main hand", "NARROW"),
    "event.entity.living.LivingIncomingDamageEvent": ("incomingDamage", "Cataclysm lowers or cancels damage", "wetness, ghost form, or a mod item on the legs",
        "onLivingAttack acts @30 with wetness, @93 with ghost_form, @149 with CURSIUM_LEGGINGS", "NARROW"),
    "event.entity.living.LivingHealEvent": ("livingHeal", "Cataclysm cancels the heal", "abyssal fear",
        "BlockHeal cancels @15 with abyssal_fear only", "NARROW"),
    "event.entity.living.LivingChangeTargetEvent": ("changeTarget", "Cataclysm lava monsters ignore the helmet", "a target with a mod item on the head",
        "onLivingSetTargetEvent @4 target not null, @73 target's HEAD is IGNITIUM_HELMET, all conditions AND", "NARROW"),
    "event.entity.living.LivingFallEvent": ("livingFall", "Cataclysm cuts fall distance", "a Player, or a mod item on the feet",
        "onLivingFall acts @35 with CURSIUM_BOOTS, @56 on a Player with HOOK_FALLING", "NARROW"),
    "event.entity.living.LivingEvent$LivingJumpEvent": ("livingJump", "Cataclysm stops the jump; Astral hurts", "stun or thorny",
        "onLivingJump @12 getEffect(stun) not null; ThornyOnEffectActiveTickProcedure#execute @21 hasEffect(thorny)", "NARROW"),
    "event.entity.living.LivingKnockBackEvent": ("livingKnockBack", "Cataclysm cancels knockback", "a NeoForge mod's mob",
        "KnockbackEvent @6 instanceof Royal_Draugr_Entity, a cataclysm class", "NARROW"),
    "event.entity.living.LivingEntityUseItemEvent$Tick": ("useItem", "Cataclysm parry window, duration", "stun, ghost form, or a mod item",
        "onUseItem (parent class) @12 stun, @27 ghost_form. onUseTick @15 @25 AZURE_SEA_SHIELD or BULWARK_OF_THE_FLAME", "NARROW"),
    "event.entity.living.LivingEntityUseItemEvent$Stop": ("useItem", "Cataclysm parry window, duration", "stun, ghost form, or a mod item",
        "onUseItem (parent class) @12 stun, @27 ghost_form. onStopUsing @15 @25 AZURE_SEA_SHIELD or BULWARK_OF_THE_FLAME", "NARROW"),
    "event.entity.living.LivingEntityUseItemEvent$Finish": ("useItem", "Cataclysm duration", "stun, ghost form, or a mod item",
        "onUseItem (parent class) @12 stun, @27 ghost_form, the only listener; the mod item test is the one shared with Tick and Stop", "NARROW"),
    "event.entity.living.MobEffectEvent$Remove": ("effectRemove", "none: compares by reference, never cancels", "ghost sickness",
        "preventEffectRemoval @19 if_acmpne of a Holder and a MobEffect, never equal, on NeoForge too. Kept for ghost_sickness as the handler means it", "NARROW"),
    "event.entity.item.ItemTossEvent": ("itemToss", "Astral closes its screen", "a mod item",
        "onItemDropped of BackpackInventoryCapability, DiaryInventoryCapability, HarshBackpackInventoryCapability @16 item is BACKPACK, DIARY, HARSH_BACKPACK", "NARROW"),
    "event.entity.player.AttackEntityEvent": ("attackEntity", "Cataclysm cancels the attack", "stun or ghost form",
        "onPlayerAttack cancels @10 with stun, @28 with ghost_form", "NARROW"),
    "event.entity.player.PlayerInteractEvent$EntityInteract": ("interact", "Cataclysm cancels", "stun",
        "onPlayerInteract3 cancels @12 with stun", "NARROW"),
    "event.entity.player.PlayerInteractEvent$LeftClickBlock": ("interact", "Cataclysm cancels", "stun",
        "onPlayerInteract5, onPlayerLeftClick cancel @12 with stun", "NARROW"),
    "event.level.BlockEvent$BreakEvent": ("blockBreak", "Cataclysm cancels; Astral awards recipes", "stun or glowstone",
        "onBreakBlock cancels @10 with stun. GlowstoneBlockBrokenByPlayerProcedure#execute @26 block at pos is GLOWSTONE, the state fireBlockBreak gets from destroyBlock", "NARROW"),
}


def load_jar(path):
    out = {}
    with zipfile.ZipFile(path) as z:
        for n in z.namelist():
            if n.endswith(".class") and not n.startswith("META-INF/"):
                ci = parse_class(z.read(n))
                out[ci.name] = ci
    return out


def load_dir(root):
    out = {}
    for d, _, fs in os.walk(root):
        for f in fs:
            if f.endswith(".class"):
                with open(os.path.join(d, f), "rb") as fh:
                    ci = parse_class(fh.read())
                out[ci.name] = ci
    return out


class Hierarchy:
    def __init__(self, classes):
        self.classes = classes
        self.memo = {EVENT: True}

    def is_event(self, name):
        r = self.memo.get(name)
        if r is None:
            self.memo[name] = False
            c = self.classes.get(name)
            r = bool(c and c.super and self.is_event(c.super))
            self.memo[name] = r
        return r

    def subclasses(self, name):
        return [c.name for c in self.classes.values() if self.is_sub(c.name, name)]

    def supers(self, name):
        """The class and its parents, up to Event."""
        while name and name != EVENT:
            yield name
            c = self.classes.get(name)
            name = c.super if c else None

    def is_sub(self, name, base):
        while name:
            if name == base:
                return True
            c = self.classes.get(name)
            name = c.super if c else None
        return False


def params(desc):
    return desc_classes(desc.split(")")[0])


def listeners(classes, h):
    """[(event, class, method)]: @SubscribeEvent methods, and method handles passed to a bus's addListener."""
    out = set()
    for c in classes.values():
        for m in c.methods:
            if any(t == SUBSCRIBE for t, _ in m.annots):
                out.update((p, c.name, m.name) for p in params(m.desc) if h.is_event(p))
            if any(x[0] in "vi" and x[2] == "addListener" for x in m.calls):
                for x in m.calls:
                    if x[0] == "h":
                        out.update((p, x[1], x[2]) for p in params(x[3]) if h.is_event(p))
    return out


def gated(classes, h):
    """Events a Nytheria Compat hook asks EventGate.has about."""
    out = set()
    for c in classes.values():
        for m in c.methods:
            if any(x[1] == EVENT_GATE and x[2] == "has" for x in m.calls):
                out.update(k for k in m.classes if h.is_event(k))
    return out


def first_tests(c, name):
    """The first three things the handler looks at, in code order: calls and class tests."""
    found = []
    for m in c.methods:
        if m.name == name:
            for x in m.calls[:4]:
                if x[0] in "visp" and x[2] != "<init>":
                    found.append(x[1].rsplit("/", 1)[-1] + "." + x[2])
            found += sorted(k.rsplit("/", 1)[-1] for k in m.classes)[:2]
            break
    return ",".join(found[:4]) or "-"


def short(name):
    return name.replace("net/neoforged/neoforge/", "").replace("/", ".")


def side(event):
    return "client" if "/client/" in event else "common"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jars", default=JARS)
    ap.add_argument("--out", default="tools/trim/event-need.tsv")
    args = ap.parse_args()

    nytheria_compat = {}
    for root in NYTHERIACOMPAT_ROOTS:
        if os.path.isdir(root):
            nytheria_compat.update(load_dir(root))
    if not nytheria_compat:
        sys.exit("no compiled classes, run ./gradlew classes first")
    mods = {}
    for f in sorted(os.listdir(args.jars)):
        for key, label in LABELS:
            if f.startswith(key):
                mods[label] = load_jar(os.path.join(args.jars, f))
    everything = dict(nytheria_compat)
    for jar in mods.values():
        everything.update(jar)
    h = Hierarchy(everything)

    own = {}
    for label, jar in list(mods.items()) + [("nytheria_compat", {k: v for k, v in nytheria_compat.items() if k.startswith(NYTHERIACOMPAT_PREFIXES)})]:
        for event, cls, method in sorted(listeners(jar, h)):
            if not event.startswith("net/neoforged/"):
                continue
            c = everything.get(cls)
            own.setdefault(event, []).append((label, cls, method, first_tests(c, method) if c else "-"))

    posted = {k: v for k, v in nytheria_compat.items() if k.startswith(NYTHERIACOMPAT_PREFIXES)}
    rows = {e: [l for p in h.supers(e) for l in own.get(p, [])] for e in set(own) | gated(posted, h)}

    with open(args.out, "w", encoding="utf-8", newline="\n") as f:
        f.write("event\tside\tlisteners\tfirst\tneed\teffect\tcondition\tproof\tverdict\n")
        for event in sorted(rows, key=short):
            ls = rows[event]
            if ls:
                need, effect, condition, proof, verdict = HAND.get(short(event), ("-", "-", "listener exists", "not analysed", "ALWAYS"))
            else:
                need, effect, condition, proof, verdict = ("-", "-", "no listener", "EventGate.has is false", "NONE")
            f.write("\t".join((short(event), side(event),
                               " ".join(f"{m}:{c.rsplit('/', 1)[-1]}#{n}" for m, c, n, _ in ls) or "-",
                               " | ".join(t for _, _, _, t in ls) or "-",
                               need, effect, condition, proof, verdict)) + "\n")
    print(f"{len(rows)} events, {sum(1 for e in rows if HAND.get(short(e), ('',) * 5)[4] == 'NARROW')} narrowed -> {args.out}")


if __name__ == "__main__":
    main()
