/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.mod.fabric

import net.neoforged.bus.api.BusBuilder
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.event.IModBusEvent
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.fml.loading.moddiscovery.ModInfo
import dev.nytheria.compat.workarounds.ForgeConfigApiPortCompat
import java.util.*
import net.fabricmc.loader.api.ModContainer as FabricModContainer

class WrappedFabricModContainer private constructor(container: FabricModContainer) : ModContainer(ModInfo(container)) {
    private val eventBus = BusBuilder.builder()
        .markerType(IModBusEvent::class.java)
        .allowPerPhasePost()
        .build()

    override fun getEventBus(): IEventBus {
        return this.eventBus
    }

    override fun <T> acceptEvent(e: T?) where T : Event?, T : IModBusEvent? {
        when (e) {
            is ModConfigEvent.Loading -> ForgeConfigApiPortCompat.fireConfigLoadEvent(this.modId, e.config)
            is ModConfigEvent.Reloading -> ForgeConfigApiPortCompat.fireConfigReloadEvent(this.modId, e.config)
            is ModConfigEvent.Unloading -> ForgeConfigApiPortCompat.fireConfigUnloadEvent(this.modId, e.config)
        }

        super.acceptEvent(e)
    }

    companion object {
        private val containers: MutableMap<FabricModContainer, WrappedFabricModContainer> = Collections.synchronizedMap(mutableMapOf<FabricModContainer, WrappedFabricModContainer>())

        @JvmStatic
        val wrappedContainers: Collection<WrappedFabricModContainer>
            get() = this.containers.values

        @JvmStatic
        fun get(container: FabricModContainer): WrappedFabricModContainer {
            synchronized(containers) {
                return containers.computeIfAbsent(container, ::WrappedFabricModContainer)
            }
        }
    }
}
