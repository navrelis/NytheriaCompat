/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.compat.neoconfig

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents
import net.neoforged.fml.config.ModConfig
import dev.nytheria.compat.workarounds.ForgeConfigApiPortCompat

class NytheriaCompatForgeConfigApiPortCompat : ForgeConfigApiPortCompat {
    override fun fireConfigLoadEvent(modId: String, config: ModConfig?) {
        NeoForgeModConfigEvents.loading(modId).invoker().onModConfigLoading(config)
    }

    override fun fireConfigReloadEvent(modId: String, config: ModConfig?) {
        NeoForgeModConfigEvents.reloading(modId).invoker().onModConfigReloading(config)
    }

    override fun fireConfigUnloadEvent(modId: String, config: ModConfig?) {
        NeoForgeModConfigEvents.unloading(modId).invoker().onModConfigUnloading(config)
    }
}
