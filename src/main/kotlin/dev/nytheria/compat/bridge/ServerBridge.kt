/*
 * Changed by the Nytheria Compat project on 2026-11-01; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.bridge

import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents
import io.github.fabricators_of_create.porting_lib.entity.events.living.LivingDropsEvent
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.neoforged.bus.api.EventPriority
import net.neoforged.neoforge.common.CommonHooks
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.EventHooks
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.server.ServerLifecycleHooks
import dev.nytheria.compat.helpers.ArchitecturyLivingDeathBridge
import dev.nytheria.compat.hook.EventGate
import dev.nytheria.compat.injections.world.entity.LivingEntityInjection
import dev.nytheria.compat.mixin.MinecraftServerAccessor

/**
 * Fabric and Porting Lib server events (lifecycle, ticks, levels, entity drops and deaths, section changes) forwarded to
 * NeoForge's event bus.
 */
object ServerBridge {
    @Suppress("removal")
    fun register() {
        ServerLifecycleEvents.SERVER_STARTED.register {
            ServerLifecycleHooks.handleServerStarted(it)
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            ServerLifecycleHooks.handleServerStopping(it)
        }

        ServerLifecycleEvents.SERVER_STOPPED.register {
            ServerLifecycleHooks.expectServerStopped()
            ServerLifecycleHooks.handleServerStopped(it)
        }

        EntityEvents.EnteringSection.EVENT.register { event ->
            CommonHooks.onEntityEnterSection(event.entity, event.packedOldPos, event.packedNewPos)
        }

        ServerTickEvents.START_SERVER_TICK.register { server ->
            EventHooks.fireServerTickPre((server as MinecraftServerAccessor)::callHaveTime, server)
        }

        ServerTickEvents.END_SERVER_TICK.register { server ->
            EventHooks.fireServerTickPost((server as MinecraftServerAccessor)::callHaveTime, server)
        }

        ServerTickEvents.START_WORLD_TICK.register { level ->
            EventHooks.fireLevelTickPre(level, (level.server as MinecraftServerAccessor)::callHaveTime)
        }

        ServerTickEvents.END_WORLD_TICK.register { level ->
            EventHooks.fireLevelTickPost(level, (level.server as MinecraftServerAccessor)::callHaveTime)
        }

        // Nytheria Compat: NeoForge's PlayerTickEvent.Pre/Post are fired by PlayerInject (Player.tick HEAD/TAIL), like NeoForge's own
        // patch. Bridging Porting Lib's PlayerTickEvent here as well fired both events twice per tick, so every
        // NeoForge player tick handler ran twice (e.g. Cataclysm's ChargeAttachment timer counted down at double speed).

        ServerWorldEvents.LOAD.register { server, level ->
            if (EventGate.has(LevelEvent.Load::class.java)) NeoForge.EVENT_BUS.post(LevelEvent.Load(level))
        }

        ServerWorldEvents.UNLOAD.register { server, level ->
            if (EventGate.has(LevelEvent.Unload::class.java)) NeoForge.EVENT_BUS.post(LevelEvent.Unload(level))
        }

        LivingDropsEvent.EVENT.register { event ->
            if (CommonHooks.onLivingDrops(event.entity, event.source, event.drops, event.isRecentlyHit))
                event.isCanceled = true
        }

        // Nytheria Compat: track LivingDeathEvent and Fabric's AFTER_DEATH per entity, so deaths of NeoForge mod entities
        //  that override die() without calling super still reach Fabric (see LivingEntityInject#nytheriacompat$reportModdedDeathToFabric).
        //  Only the event object is stored, its cancellation state is read once the whole die() call is done.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDeathEvent::class.java) { event ->
            (event.entity as LivingEntityInjection).`nytheriacompat$setPendingDeathEvent`(event)
        }

        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            (entity as LivingEntityInjection).`nytheriacompat$markFabricDeathReported`()
        }

        // Nytheria Compat: the same gap for Architectury's EntityEvent.LIVING_DEATH (FTB Quests kill tasks), only if Architectury is loaded.
        ArchitecturyLivingDeathBridge.register()
    }
}
