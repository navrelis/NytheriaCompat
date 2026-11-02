/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.workarounds

import net.fabricmc.loader.api.FabricLoader
import net.neoforged.fml.config.ModConfig

// Little workaround since I don't want the Forge Config API Port code in this main codebase.
interface ForgeConfigApiPortCompat {
    fun fireConfigLoadEvent(modId: String, config: ModConfig?)
    fun fireConfigReloadEvent(modId: String, config: ModConfig?)
    fun fireConfigUnloadEvent(modId: String, config: ModConfig?)

    companion object : ForgeConfigApiPortCompat {
        val instance: ForgeConfigApiPortCompat? by lazy {
            if (FabricLoader.getInstance().isModLoaded("forgeconfigapiport")) {
                try {
                    Class.forName("dev.nytheria.compat.compat.neoconfig.NytheriaCompatForgeConfigApiPortCompat").getDeclaredConstructor().newInstance() as? ForgeConfigApiPortCompat
                } catch (_: Throwable) { null }
            } else null
        }

        override fun fireConfigLoadEvent(modId: String, config: ModConfig?) {
            instance?.fireConfigLoadEvent(modId, config)
        }

        override fun fireConfigReloadEvent(modId: String, config: ModConfig?) {
            instance?.fireConfigReloadEvent(modId, config)
        }

        override fun fireConfigUnloadEvent(modId: String, config: ModConfig?) {
            instance?.fireConfigUnloadEvent(modId, config)
        }
    }
}
