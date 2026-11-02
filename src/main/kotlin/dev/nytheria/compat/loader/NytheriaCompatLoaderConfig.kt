/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader

import net.fabricmc.loader.api.FabricLoader
import dev.nytheria.compat.shaded.cichlidmc.tinycodecs.api.codec.Codec
import java.nio.file.Path

@JvmRecord
data class NytheriaCompatLoaderConfig(
    /**
     * Represents the list of mod IDs that should not be getting resolved by Nytheria Compat.
     */
    val forceDisabledModIds: List<String> = emptyList()
) {
    companion object {
        val PATH: Path = FabricLoader.getInstance().configDir.resolve("nytheria_compat_overrides.json")
        // The file name of Sporran 1.0.8, read when the new file does not exist. rename:keep
        val OLD_PATH: Path = FabricLoader.getInstance().configDir.resolve("sporran_overrides.json") // rename:keep
        val CODEC: Codec<NytheriaCompatLoaderConfig> = Codec.STRING.listOf().optional(emptyList())
            .fieldOf("force_disabled_mods").asCodec().xmap(::NytheriaCompatLoaderConfig, NytheriaCompatLoaderConfig::forceDisabledModIds)
    }
}
