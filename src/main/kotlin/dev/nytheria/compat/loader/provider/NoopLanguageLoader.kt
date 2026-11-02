/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.provider

import net.neoforged.fml.ModContainer
import net.neoforged.neoforgespi.language.IModInfo
import net.neoforged.neoforgespi.language.IModLanguageLoader
import net.neoforged.neoforgespi.language.ModFileScanData

object NoopLanguageLoader : IModLanguageLoader {
    override fun name(): String = "nytheria_compat_noop"

    override fun version(): String = "0.0.0"

    override fun loadMod(
        info: IModInfo?,
        modFileScanResults: ModFileScanData?,
        layer: ModuleLayer?
    ): ModContainer? {
        throw IllegalStateException()
    }
}
