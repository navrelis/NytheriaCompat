package dev.nytheria.compat.loader

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.toml.TomlParser
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipInputStream

/**
 * Nytheria Compat: what is read from a NeoForge mod jar, apart from anything that needs the game. NeoForge only reads
 * `META-INF/neoforge.mods.toml`: a `META-INF/mods.toml` next to it (Astral Dimension has one) is stale Forge metadata.
 */
internal object ModJars {
    private const val MODS_TOML = "META-INF/neoforge.mods.toml"
    private const val DEFAULT_ACCESS_TRANSFORMER = "META-INF/accesstransformer.cfg"

    fun modsToml(jar: JarFile): JarEntry? = jar.getJarEntry(MODS_TOML)

    /** The mod ids of the NeoForge mods nested in this jar, at any depth. [jarName] starts the path each one is reported under. */
    fun nestedMods(jar: JarFile, jarName: String): List<ModLock.FoundMod> {
        return jar.entries().asSequence().filter { it.name.endsWith(".jar") }.flatMap { entry ->
            jar.getInputStream(entry).use { nestedMods(ZipInputStream(it), "$jarName!/${entry.name}") }
        }.toList()
    }

    private fun nestedMods(zip: ZipInputStream, path: String): List<ModLock.FoundMod> {
        val found = mutableListOf<ModLock.FoundMod>()

        while (true) {
            val entry = zip.nextEntry ?: return found

            if (entry.name == MODS_TOML) {
                val toml = TomlParser().parse(zip.readAllBytes().inputStream(), Charsets.UTF_8)
                toml.get<List<CommentedConfig>>("mods").orEmpty().mapNotNullTo(found) { mod ->
                    mod.get<String>("modId")?.let { ModLock.FoundMod(path, it, false) }
                }
            } else if (entry.name.endsWith(".jar")) {
                found += nestedMods(ZipInputStream(zip.readAllBytes().inputStream()), "$path!/${entry.name}")
            }
        }
    }

    /**
     * The default path is read even when the mods.toml has no `[[accessTransformers]]` (Astral Dimension, Curios), and only
     * once when it also lists it (Cataclysm, Lionfish API).
     */
    fun readAccessTransformers(jar: JarFile, declared: List<String>, onMissing: (String) -> Unit): List<ByteArray> {
        return (listOf(DEFAULT_ACCESS_TRANSFORMER) + declared).distinct().mapNotNull { path ->
            val entry = jar.getJarEntry(path)
            if (entry == null && path != DEFAULT_ACCESS_TRANSFORMER)
                onMissing(path)

            entry?.let { jar.getInputStream(it).use { input -> input.readAllBytes() } }
        }
    }
}
