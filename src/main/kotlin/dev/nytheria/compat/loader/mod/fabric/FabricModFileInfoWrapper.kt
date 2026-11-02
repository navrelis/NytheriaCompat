/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.mod.fabric

import net.fabricmc.loader.api.ModContainer
import net.neoforged.neoforgespi.language.IConfigurable
import net.neoforged.neoforgespi.language.IModFileInfo
import net.neoforged.neoforgespi.language.IModInfo
import net.neoforged.neoforgespi.locating.IModFile

class FabricModFileInfoWrapper(val mod: ModContainer) : IModFileInfo {
    override fun getMods(): MutableList<IModInfo> {
        return mutableListOf()
    }

    override fun requiredLanguageLoaders(): List<IModFileInfo.LanguageSpec> {
        return emptyList()
    }

    override fun showAsResourcePack(): Boolean {
        return false
    }

    override fun showAsDataPack(): Boolean {
        return false
    }

    override fun getFileProperties(): MutableMap<String, Any> {
        return mutableMapOf()
    }

    override fun getLicense(): String? {
        return mod.metadata.license.joinToString(",")
    }

    override fun moduleName(): String? {
        return mod.metadata.id
    }

    override fun versionString(): String? {
        return mod.metadata.version.friendlyString
    }

    override fun usesServices(): List<String?>? {
        return emptyList()
    }

    override fun getFile(): IModFile? {
        return FabricModFileWrapper(this.mod, this)
    }

    override fun getConfig(): IConfigurable? {
        return null
    }
}