/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.mod

import cpw.mods.modlauncher.api.IEnvironment
import cpw.mods.modlauncher.api.ILaunchHandlerService
import cpw.mods.modlauncher.api.IModuleLayerManager
import cpw.mods.modlauncher.api.TypesafeMap
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService
import java.util.*
import java.util.function.Function

class NytheriaCompatEnvironment : IEnvironment {
    private val environment = TypesafeMap(IEnvironment::class.java)

    override fun <T> getProperty(key: TypesafeMap.Key<T>): Optional<T & Any> {
        return environment[key]
    }

    override fun <T> computePropertyIfAbsent(
        key: TypesafeMap.Key<T>,
        valueFunction: Function<in TypesafeMap.Key<T>, out T>
    ): T {
        return environment.computeIfAbsent(key, valueFunction)
    }

    // We implement like. none of these.
    override fun findLaunchPlugin(name: String): Optional<ILaunchPluginService> {
        return Optional.empty()
    }

    override fun findLaunchHandler(name: String): Optional<ILaunchHandlerService> {
        return Optional.empty()
    }

    override fun findModuleLayerManager(): Optional<IModuleLayerManager> {
        return Optional.empty()
    }

}