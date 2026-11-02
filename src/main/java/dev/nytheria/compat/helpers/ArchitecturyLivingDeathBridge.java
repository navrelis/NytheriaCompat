package dev.nytheria.compat.helpers;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import dev.nytheria.compat.NytheriaCompat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// Nytheria Compat: Architectury API on Fabric fires EntityEvent.LIVING_DEATH from its own mixin at the HEAD of
//  LivingEntity/Player/ServerPlayer.die. NeoForge mod entities that override die() without calling super
//  (e.g. L_Ender's Cataclysm's Animation_Monsters) only post LivingDeathEvent themselves, so Architectury mods
//  (FTB Quests kill tasks, ...) never saw those deaths. For every LivingDeathEvent posted outside a vanilla die(),
//  this does what Architectury does on NeoForge (EventHandlerImplCommon): invoke LIVING_DEATH from a HIGH priority
//  listener and cancel the event if the result is false. Architectury is only accessed reflectively.
public final class ArchitecturyLivingDeathBridge {
    // (LivingEntity, DamageSource) -> EntityEvent.LIVING_DEATH.invoker().die(entity, source).isFalse()
    private static MethodHandle livingDeathIsFalse;

    private ArchitecturyLivingDeathBridge() {}

    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded("architectury"))
            return;

        try {
            var loader = ArchitecturyLivingDeathBridge.class.getClassLoader();
            var lookup = MethodHandles.publicLookup();
            var eventClass = Class.forName("dev.architectury.event.Event", false, loader);
            var eventResultClass = Class.forName("dev.architectury.event.EventResult", false, loader);
            var livingDeathClass = Class.forName("dev.architectury.event.events.common.EntityEvent$LivingDeath", false, loader);
            var livingDeathEvent = Class.forName("dev.architectury.event.events.common.EntityEvent", true, loader)
                .getField("LIVING_DEATH").get(null);

            // The invoker is fetched on every call, like Architectury itself does, as it is rebuilt when listeners register.
            var invoker = lookup.findVirtual(eventClass, "invoker", MethodType.methodType(Object.class))
                .bindTo(livingDeathEvent)
                .asType(MethodType.methodType(livingDeathClass));
            var die = lookup.findVirtual(livingDeathClass, "die", MethodType.methodType(eventResultClass, LivingEntity.class, DamageSource.class));
            var isFalse = lookup.findVirtual(eventResultClass, "isFalse", MethodType.methodType(boolean.class));

            livingDeathIsFalse = MethodHandles.collectArguments(MethodHandles.filterReturnValue(die, isFalse), 0, invoker);
        } catch (ReflectiveOperationException | RuntimeException e) {
            NytheriaCompat.Companion.getLogger().error("Failed to hook Architectury's EntityEvent.LIVING_DEATH, deaths of NeoForge mod entities that skip super.die() will not reach it", e);
            return;
        }

        // Same priority and cancelled-event handling as Architectury's NeoForge listener.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingDeathEvent.class, ArchitecturyLivingDeathBridge::onLivingDeath);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        var entity = event.getEntity();

        // Posted from vanilla die(), where Architectury's own die() mixin fires (or already fired) LIVING_DEATH.
        if (entity.nytheriacompat$isPostingVanillaLivingDeath())
            return;

        boolean cancel;
        try {
            cancel = (boolean) livingDeathIsFalse.invokeExact(entity, event.getSource());
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        if (cancel)
            event.setCanceled(true);
    }
}
