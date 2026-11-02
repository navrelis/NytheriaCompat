/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.javafmlmod.FMLModContainer
import dev.nytheria.compat.loader.mod.NeoForgeMod

open class NytheriaCompatModContainer(internal val mod: NeoForgeMod) : FMLModContainer(mod, emptyList(), mod.scanData, ModuleLayer.empty()) {
    override fun getEventBus(): IEventBus {
        return mod.eventBus
    }
}
