/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap

import net.fabricmc.loader.api.FabricLoader
import net.minecraftforge.fart.api.ClassProvider
import net.minecraftforge.fart.internal.EnhancedRemapper
import net.minecraftforge.srgutils.IMappingFile
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Supplier

class NytheriaCompatEnhancedRemapper(val provider: ClassProvider, private val file: IMappingFile, log: Consumer<String>, private val devClassProvider: Supplier<ClassProvider>) : EnhancedRemapper(provider, file, log) {
    fun mapMethodNamePrefixDesc(
        owner: String,
        name: String,
        descPrefix: String
    ): String? {
        val hierarchy = getClassHierarchy(owner)

        for (info in hierarchy) {
            for (methodInfo in info.methods) {
                if (methodInfo.name == name && methodInfo.descriptor.startsWith(descPrefix)) {
                    return this.mapMethodName(info.name, methodInfo.name, methodInfo.descriptor)
                }
            }
        }

        val cls = file.classes.firstOrNull { it.original == owner } ?: return this.mapMethodName(owner, name, descPrefix)
        for (method in cls.methods) {
            if (method.original == name && method.descriptor.startsWith(descPrefix)) {
                return method.mapped
            }
        }

        return this.mapMethodName(owner, name, descPrefix)
    }

    private val mappingResolver = FabricLoader.getInstance().mappingResolver
    private val shouldTryRemap = mappingResolver.currentRuntimeNamespace != "intermediary"
    private lateinit var devRemapper: EnhancedRemapper

    fun initDevRemapper() {
        if (!::devRemapper.isInitialized) {
            devRemapper = EnhancedRemapper(devClassProvider.get(), NytheriaCompatRemapper.fabricMappings.getMap("intermediary", "named")) {}
        }
    }

    override fun map(name: String): String {
        if (FabricLoader.getInstance().isDevelopmentEnvironment && !NytheriaCompatRemapper.forceProductionRemap) {
            return name
        }

        val intermediary = super.map(name)

        if (shouldTryRemap) {
            initDevRemapper()
            return devRemapper.map(intermediary)
        }

        return intermediary
    }

    override fun mapFieldName(owner: String, name: String, descriptor: String): String {
        if (FabricLoader.getInstance().isDevelopmentEnvironment && !NytheriaCompatRemapper.forceProductionRemap && !name.startsWith("this$")) {
            return name
        }

        val intermediary = super.mapFieldName(owner, name, descriptor)

        if (shouldTryRemap && (intermediary.startsWith("field_") || intermediary.startsWith("comp_"))) {
            initDevRemapper()
            for (info in getClassHierarchy(owner)) {
                val mapped = devRemapper.mapFieldName(NytheriaCompatRemapper.remapClass(info.name, toIntermediary = true, ignoreWorkaround = true), intermediary, NytheriaCompatRemapper.remapDescriptor(descriptor, toIntermediary = true))

                if (mapped != intermediary)
                    return mapped
            }
        }

        // Special handling for production libraries that rely on Intermediary-mapped libraries
        if (!shouldTryRemap && intermediary == name) {
            val hierarchy = getClassHierarchy(owner)
            for (info in hierarchy) {
                val mapped = super.mapFieldName(NytheriaCompatRemapper.unmapClass(info.name), name, descriptor)

                if (mapped != intermediary)
                    return mapped
            }
        }

        return intermediary
    }

    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        // really, really dumb remap because some mods actually call into this, unfortunately.
        fun matchesSelf(className: String): Boolean {
            return name == "self" && descriptor == "()L$className;" && getClassHierarchy(owner).any { it.name == className || it.`super` == className }
        }

        if (matchesSelf("net/minecraft/world/entity/LivingEntity")
            || matchesSelf("net/minecraft/network/protocol/PacketFlow")
            || matchesSelf("net/minecraft/server/level/ServerChunkCache")
            || matchesSelf("net/minecraft/server/players/PlayerList")
        )
            return $$"neoforge$self"

        if (FabricLoader.getInstance().isDevelopmentEnvironment && !NytheriaCompatRemapper.forceProductionRemap && !name.startsWith("lambda$")) {
            return name
        }

        val intermediary = super.mapMethodName(owner, name, descriptor)

        // Special handling for dev environments
        if (shouldTryRemap && (intermediary.startsWith("method_") || intermediary.startsWith("comp_"))) {
            initDevRemapper()

            for (info in getClassHierarchy(owner)) {
                val mapped = devRemapper.mapMethodName(NytheriaCompatRemapper.remapClass(info.name, toIntermediary = true, ignoreWorkaround = true), intermediary, NytheriaCompatRemapper.remapDescriptor(descriptor, toIntermediary = true))

                if (mapped != intermediary)
                    return mapped
            }
        }

        // Special handling for production libraries that rely on Intermediary-mapped libraries
        if (!shouldTryRemap && intermediary == name) {
            val hierarchy = getClassHierarchy(owner)
            for (info in hierarchy) {
                val mapped = super.mapMethodName(NytheriaCompatRemapper.unmapClass(info.name), name, descriptor)

                if (mapped != intermediary)
                    return mapped
            }
        }

        return intermediary
    }

    private fun mapToMojang(name: String): String {
        if (name.startsWith("net/minecraft/class_")) {
            return NytheriaCompatRemapper.unmapClass(name)
        }

        return name
    }

    // Nytheria Compat: getClassHierarchy runs for nearly every field and method reference that is remapped (and again for every
    //  class the coremod inverse remapper touches), and FART's ClassProvider re-reads and re-parses the class file from
    //  its jar on every getClass call, because it is not built with shouldCacheAll. That made up ~80% of the time spent
    //  remapping a mod. The provider only reads jars that never change while this remapper exists, so both lookups can
    //  be cached. The caches are thread-safe (mods are remapped concurrently) and are dropped after remapping, see
    //  [clearCaches].
    private val classInfoCache = ConcurrentHashMap<String, Optional<out ClassProvider.IClassInfo>>()
    private val hierarchyCache = ConcurrentHashMap<String, List<ClassProvider.IClassInfo>>()

    private fun getClassInfo(name: String): ClassProvider.IClassInfo? {
        return classInfoCache.computeIfAbsent(name) { provider.getClass(it) }.orElse(null)
    }

    fun clearCaches() {
        classInfoCache.clear()
        hierarchyCache.clear()
    }

    fun getClassHierarchy(name: String): List<ClassProvider.IClassInfo> {
        // Nytheria Compat: not computeIfAbsent, computing a hierarchy recurses into the interfaces.
        hierarchyCache[name]?.let { return it }

        val hierarchy = Collections.unmodifiableList(computeClassHierarchy(name))
        return hierarchyCache.putIfAbsent(name, hierarchy) ?: hierarchy
    }

    private fun computeClassHierarchy(name: String): List<ClassProvider.IClassInfo> {
        val hierarchy = mutableListOf<ClassProvider.IClassInfo>()

        var currentClass = getClassInfo(mapToMojang(name)) ?: return emptyList()
        hierarchy.add(currentClass)

        do {
            if (currentClass.`super` == null) {
                break
            }

            for (itf in currentClass.interfaces) {
                hierarchy.addAll(getClassHierarchy(itf))
            }

            currentClass = getClassInfo(mapToMojang(currentClass.`super` ?: break)) ?: break

            if (currentClass.name.startsWith("java/lang/") || currentClass.name.startsWith("com/google/")) {
                break
            }

            hierarchy.add(currentClass)
        } while (true)

        return hierarchy
    }
}
