/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.MappingResolver

class NoopMappingResolver : MappingResolver {
    override fun getNamespaces(): MutableCollection<String> {
        return mutableListOf("intermediary")
    }

    override fun getCurrentRuntimeNamespace(): String {
        return FabricLoader.getInstance().mappingResolver.currentRuntimeNamespace
    }

    override fun mapClassName(namespace: String, className: String): String {
        return className
    }

    override fun unmapClassName(targetNamespace: String?, className: String): String {
        return className
    }

    override fun mapFieldName(namespace: String?, owner: String?, name: String, descriptor: String?): String {
        return name
    }

    override fun mapMethodName(namespace: String?, owner: String?, name: String, descriptor: String?): String {
        return name
    }
}