/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-08; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.toml.TomlParser
import cpw.mods.modlauncher.Launcher
import cpw.mods.modlauncher.api.IEnvironment
import cpw.mods.niofs.union.KiltUnionFileSystemHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.stream.consumeAsFlow
import kotlinx.coroutines.withContext
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.fabricmc.loader.impl.ModContainerImpl
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.Event
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.fml.loading.moddiscovery.ModFileInfo
import net.neoforged.fml.loading.moddiscovery.NightConfigWrapper
import net.neoforged.fml.loading.modscan.ModClassVisitor
import net.neoforged.fml.loading.toposort.TopologicalSort
import net.neoforged.neoforgespi.Environment
import net.neoforged.neoforgespi.language.IModInfo
import net.neoforged.neoforgespi.language.IModLanguageLoader
import net.neoforged.neoforgespi.language.MavenVersionAdapter
import net.neoforged.neoforgespi.language.ModFileScanData
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.spongepowered.asm.mixin.FabricUtil
import dev.nytheria.compat.NytheriaCompat
import dev.nytheria.compat.loader.asm.AccessTransformerLoader
import dev.nytheria.compat.loader.asm.EnumExtensionLoader
import dev.nytheria.compat.loader.mod.NytheriaCompatEnvironment
import dev.nytheria.compat.loader.mod.NeoForgeMod
import dev.nytheria.compat.loader.mod.NeoForgeModVersion
import dev.nytheria.compat.loader.mod.NeoForgeVersionConstraint
import dev.nytheria.compat.loader.provider.NoopLanguageLoader
import dev.nytheria.compat.loader.remap.NytheriaCompatRemapper
import dev.nytheria.compat.shaded.cichlidmc.tinyjson.TinyJson
import dev.nytheria.compat.util.DistUtil
import dev.nytheria.compat.util.NytheriaCompatHelper
import dev.nytheria.compat.util.NytheriaCompatTimings
import dev.nytheria.compat.util.buildGraph
import xyz.bluspring.knit.loader.KnitLoader
import xyz.bluspring.knit.loader.KnitModLoader
import xyz.bluspring.knit.loader.mod.ModDefinition
import xyz.bluspring.knit.loader.mod.ModDependency
import xyz.bluspring.knit.loader.mod.ModEnvironment
import xyz.bluspring.knit.loader.util.*
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.spi.FileSystemProvider
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile
import java.util.jar.Manifest
import kotlin.io.path.*

class NytheriaCompatLoader : KnitModLoader<NeoForgeMod>(NytheriaCompat.MOD_ID, "NeoForge") {
    private val tomlParser = TomlParser()
    private val loadedModIds = mutableSetOf<String>()

    private val environment = NytheriaCompatEnvironment()
    var config = NytheriaCompatLoaderConfig()

    val languageLoaders: Collection<IModLanguageLoader> by lazy {
        ServiceLoader.load(IModLanguageLoader::class.java).toList()
    }

    // Otherwise, Neo mods trying to target local names end up failing miserably,
    // due to NeoForge's mixin version being on 0.15.2, which notably misses this commit in 0.17.0
    // that fixes the actual parameter names - https://github.com/FabricMC/Mixin/commit/41a0eadd1847e03e1405811b3209badc98c597de
    override val fabricMixinCompatibilityVersion: Int = FabricUtil.COMPATIBILITY_0_14_0

    init {
        // If someone wants to fix this, be my guest, drop a PR. Don't send threats of forking Nytheria Compat just because you don't like the fact that we don't support Embeddium.
        if (FabricLoader.getInstance().isModLoaded("embeddium")) {
            KnitLoader.instance.displayError(NYTHERIACOMPAT_ERROR_MESSAGE, IllegalStateException("Nytheria Compat: You are using Embeddium, which is not supported under Nytheria Compat!"))
        }

        this.loadConfig()
    }

    fun loadConfig() {
        val path = if (NytheriaCompatLoaderConfig.PATH.exists()) NytheriaCompatLoaderConfig.PATH else NytheriaCompatLoaderConfig.OLD_PATH
        if (path.exists()) {
            try {
                NytheriaCompatLoaderConfig.CODEC.decode(TinyJson.parse(path.reader(options = arrayOf(StandardOpenOption.READ))))
                    .ifPresentOrElse({
                        this.config = it
                    }, {
                        NytheriaCompat.logger.error("An error occurred while trying to load the Nytheria Compat loader config! Error: $it")
                    })
            } catch (e: Throwable) {
                NytheriaCompat.logger.error("An error occurred while trying to load the Nytheria Compat loader config!", e)
            }
        }
    }

    override fun getModDefinitions(path: Path): List<ModDefinition> {
        if (path.extension != "jar")
            return emptyList()

        // Nytheria Compat: a jar that Fabric Loader already runs as a Fabric mod is not loaded a second time as a NeoForge mod,
        //  even when its NeoForge mod ID differs (betterbiomeblend / betterbiomereblend). Many multi-loader jars ship a
        //  neoforge.mods.toml that is unfilled (${mod_id}) or only meant for NeoForge. Knit's own check for this
        //  (canModLoadNatively) never matches, because Fabric Loader drops its mod candidate list after setup.
        fabricCodeJars[normalizePath(path)]?.let { fabricId ->
            NytheriaCompat.logger.debug("Nytheria Compat: {} is already loaded as the Fabric mod {}, ignoring its NeoForge metadata", path.fileName, fabricId)
            fabricRunJars += ModLock.FoundMod(path.name, fabricId, true)
            return emptyList()
        }

        return NytheriaCompatTimings.time("discovery of ${path.fileName}") { loadModDefinitions(path) }
    }

    // Nytheria Compat: stops the game unless this is the Nytheria modpack, see ModLock. This runs before any NeoForge mod is on the class
    //  path. Only the ID of the translator's built-in mod is exempt, so a built-in added any other way goes through the same list.
    override fun checkDefinitions(definitions: Collection<ModDefinition>) {
        val found = fabricRunJars.toMutableList()

        for (definition in definitions) {
            if (definition.id == NEOFORGE_MOD_ID)
                continue

            val path = definition.originalPath
            found += ModLock.FoundMod(path.name, definition.id, false)
            if (path.extension == "jar")
                JarFile(path.toFile()).use { found += ModJars.nestedMods(it, path.name) }
        }

        val problems = ModLock.check(found, FabricLoader.getInstance().allMods.map { ModLock.FabricMod(it.metadata.id, it.metadata.provides) })
        if (problems.isNotEmpty()) {
            val message = (listOf(ModLock.REFUSED_TEXT) + problems).joinToString("\n")
            NytheriaCompat.logger.error(message)
            KnitLoader.instance.displayError(message, IllegalStateException(message))
        }
    }

    private val fabricRunJars = mutableListOf<ModLock.FoundMod>()

    // Nytheria Compat: every top-level jar Fabric Loader took as a mod, with code or without.
    private val fabricJars: Set<String> by lazy {
        FabricLoader.getInstance().allMods.filter { it.origin.kind == ModOrigin.Kind.PATH }
            .flatMap { it.origin.paths }.map(::normalizePath).toSet()
    }

    // Nytheria Compat: top-level jars that Fabric Loader runs as a Fabric mod with code of its own (entrypoints, mixins or
    //  language adapters), mapped to the Fabric mod ID. Jars whose fabric.mod.json is only a stub are left out.
    private val fabricCodeJars: Map<String, String> by lazy {
        val env = FabricLoader.getInstance().environmentType
        val jars = mutableMapOf<String, String>()

        for (container in FabricLoader.getInstance().allMods) {
            if (container.origin.kind != ModOrigin.Kind.PATH)
                continue

            val metadata = (container as? ModContainerImpl)?.metadata ?: continue
            val hasCode = metadata.entrypointKeys.isNotEmpty() || metadata.getMixinConfigs(env).isNotEmpty() ||
                metadata.languageAdapterDefinitions.isNotEmpty()
            if (!hasCode)
                continue

            for (originPath in container.origin.paths) {
                if (originPath.extension == "jar")
                    jars[normalizePath(originPath)] = container.metadata.id
            }
        }

        jars
    }

    private fun normalizePath(path: Path): String {
        val normalized = runCatching { path.toRealPath() }.getOrElse { path.toAbsolutePath().normalize() }.toString()
        return if (System.getProperty("os.name").startsWith("Windows")) normalized.lowercase() else normalized
    }

    override fun getNativeModId(dependencyId: String, nativeLoaderName: String): String {
        val loader = FabricLoader.getInstance()
        if (loader.isModLoaded(dependencyId))
            return dependencyId

        // fun times.
        if (loader.isModLoaded(dependencyId.replace("_", "-")))
            return dependencyId.replace("_", "-")
        else if (loader.isModLoaded(dependencyId.replace("_", "")))
            return dependencyId.replace("_", "")

        return super.getNativeModId(dependencyId, nativeLoaderName)
    }

    private fun loadModDefinitions(path: Path): List<ModDefinition> {
        JarFile(path.toFile()).use { jarFile ->
            // Prevent users from having both Nytheria Compat and Connector at the same time.
            if (jarFile.getEntry("org/sinytra/connector/ConnectorUtil.class") != null) {
                throw Exception("Sinytra Connector was detected! I know I said \"Isn't it reasonable to have both?\", but come on!")
            }

            // No neoforge.mods.toml means no NeoForge mod.
            val modsToml = ModJars.modsToml(jarFile) ?: return emptyList()

            // A Fabric mod that Fabric Loader left out (a client-only dual-loader jar on a server, the older of two copies) is not a NeoForge mod either.
            if (jarFile.getEntry("fabric.mod.json") != null && normalizePath(path) !in fabricJars)
                return emptyList()

            // Try to load manifest from JAR file, because it's required for some stuff in Forge mods.
            val manifest = runCatching { jarFile.getInputStream(jarFile.getEntry("META-INF/MANIFEST.MF")).use { Manifest(it) } }.getOrNull()

            // Load all mod definitions from the TOML.
            return jarFile.getInputStream(modsToml).use { parseModsToml(path, tomlParser.parse(it, Charsets.UTF_8), manifest) }
        }
    }

    private fun parseModsToml(path: Path, toml: CommentedConfig, manifest: Manifest?, parentId: String? = null, isBuiltIn: Boolean = false): List<ModDefinition> {
        val fileName = path.fileName
        val modLoader = toml.get<String>("modLoader")

        val loaderVersionRange = MavenVersionAdapter.createFromVersionSpec(toml.get("loaderVersion"))
        when (modLoader) {
            "javafml", "lowcodefml" -> {
                if (!loaderVersionRange.containsVersion(SUPPORTED_FML_VERSION)) {
                    throw IncompatibleModException("NeoForge mod file $fileName does not support FML version ${SUPPORTED_FML_VERSION}! (mod supports versions between [$loaderVersionRange])")
                }
            }
        }

        val definitions = mutableListOf<ModDefinition>()
        val mainConfig = NightConfigWrapper(toml)

        // Load all mod metadata in the TOML, since Forge allows mods to specify multiple mods in the TOML.
        for (metadata in mainConfig.getConfigList("mods")) {
            val modId = metadata.getConfigElement<String>("modId").orElseThrow {
                Exception("NeoForge mod file $fileName does not contain a mod ID!")
            }

            if (this.config.forceDisabledModIds.contains(modId)) {
                NytheriaCompat.logger.info("Mod ID $modId was detected to be forcefully disabled in the config, skipping.")
                continue
            }

            val modVersion = NeoForgeModVersion(DefaultArtifactVersion(
                // Forge custom-replaces mod versions with string templates, so we need to handle that.
                metadata.getConfigElement<String>("version").orElse("1")
                    .run {
                        if (this == "\${file.jarVersion}")
                            manifest?.mainAttributes?.getValue("Implementation-Version") ?: "0.0NONE"
                        else if (this == "\${global.neoForgeVersion}")
                            SUPPORTED_NEO_API_VERSION.toString()
                        else if (this == "\${global.mcVersion}")
                            MC_VERSION.friendlyString
                        else this
                    }
            ))

            val dependencies = mutableListOf<ModDependency>()

            // Check all dependencies from the provided mod
            for (neoDep in mainConfig.getConfigList("dependencies", modId)) {
                val depId = neoDep.getConfigElement<String>("modId").orElseThrow {
                    Exception("NeoForge mod file $fileName's dependencies contain a dependency without a mod ID!")
                }
                val versionRange = MavenVersionAdapter.createFromVersionSpec(
                    neoDep.getConfigElement<String>("versionRange")
                        .map {
                            if (depId == "minecraft" && (it.startsWith("[1.21,") || it == "[1.21]"))
                                "[1.21,1.21.2)" // Neo, what the fuck? (https://github.com/neoforged/FancyModLoader/blob/1.21.1/loader/src/main/java/net/neoforged/fml/loading/VersionSupportMatrix.java)
                            else it
                        }
                        .orElse("[0,)") // sure
                )

                dependencies.add(ModDependency(
                    id = depId,
                    type = when (neoDep.getConfigElement<String>("type").orElse("optional").lowercase()) {
                        "required" -> ModDependency.Type.REQUIRED
                        "optional" -> ModDependency.Type.OPTIONAL
                        "discouraged" -> ModDependency.Type.DISCOURAGED
                        "incompatible" -> ModDependency.Type.INCOMPATIBLE

                        else -> ModDependency.Type.OPTIONAL
                    },
                    constraint = NeoForgeVersionConstraint(versionRange),

                    // Forge has sided dependencies. How did we get sided dependencies before sided mods?
                    side = when (neoDep.getConfigElement<String>("side").orElse("BOTH")) {
                        "CLIENT" -> ModEnvironment.CLIENT
                        "SERVER" -> ModEnvironment.SERVER
                        "BOTH" -> ModEnvironment.BOTH
                        else -> throw IllegalArgumentException("Invalid side ${neoDep.getConfigElement<String>("side")} provided while handling Forge mod file $fileName!")
                    },

                    // Knit has no reason to handle ordering, but we do, so we store it into the additional data.
                    additionalData = mapOf(
                        "ordering" to IModInfo.Ordering.valueOf(neoDep.getConfigElement<String>("ordering").orElse("NONE"))
                    )
                ))
            }

            val mixinConfigs = (manifest?.mainAttributes?.getValue("MixinConfigs")?.split(",")
                ?.filter { !it.trim().isBlank() } // why the FUCK is this a possibility.
                ?.map { ModDefinition.MixinConfig(it) }
                ?: emptyList()).toMutableSet()

            for (mixinConfig in mainConfig.getConfigList("mixins")) {
                mixinConfigs.add(ModDefinition.MixinConfig(
                    mixinConfig.getConfigElement<String>("config").orElse(null) ?: continue
                ))
            }

            val accessTransformerFiles = mutableSetOf<String>()
            for (accessTransformer in mainConfig.getConfigList("accessTransformers")) {
                accessTransformerFiles.add(accessTransformer.getConfigElement<String>("file").orElse(null) ?: continue)
            }

            val definition = ModDefinition(
                id = modId,
                displayName = metadata.getConfigElement<String>("displayName").orElse(modId),
                description = metadata.getConfigElement<String>("description").orElse("")
                    .replace("\r", ""), // Otherwise, the CR gets rendered weirdly into the newlines.
                authors = try {
                    metadata.getConfigElement<String>("authors").orElse("").split(",")
                } catch (_: ClassCastException) {
                    // this is apparently a possibility that I didn't know about? huh.
                    metadata.getConfigElement<List<String>>("authors").orElse(listOf())
                },
                version = modVersion,
                license = toml.get("license"),

                dependencies = dependencies,
                mixinConfigs = mixinConfigs.toList(),
                path = path,

                // Sets the parent ID of the mod definition
                parentId = parentId,

                icon = metadata.getConfigElement<String>("logoFile").orElse(mainConfig.getConfigElement<String>("logoFile").orElse("")) ?: "",

                // Forge mods handle both, there's no way to define sided mods.
                environment = ModEnvironment.BOTH,

                // If this mod is built-in, make sure to specify it.
                isBuiltin = isBuiltIn,

                additionalData = mapOf(
                    "manifest" to manifest,
                    "config" to mainConfig,
                    "loader" to modLoader,
                    "accessTransformers" to accessTransformerFiles.toList(),
                ),

                loaderCustomData = mapOf(
                    "mcb" to listOf<Map<String, Any>>(
                        // https://syorito-hatsuki.github.io/modmenu-badges-lib/
                        mapOf(
                            "name" to "NeoForge",
                            "labelColor" to argb(255, 255, 255),
                            "outlineColor" to argb(207, 128, 55),
                            "fillColor" to argb(136, 60, 18),
                        )
                    )
                )
            )

            definitions.add(definition)
        }

        return definitions
    }

    private fun argb(r: Int, g: Int, b: Int, a: Int = 255): Int {
        // 0xFF_FF_FF_FF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun getBuiltinModDefinitions(): List<ModDefinition> {
        return if (FabricLoader.getInstance().isDevelopmentEnvironment) {
            val modsList = mutableListOf<ModDefinition>()

            for (url in this::class.java.classLoader.getResources("META-INF/nytheria_compat_neoforge.mods.toml")) {
                val toml = tomlParser.parse(url)
                modsList.addAll(parseModsToml(NytheriaCompatLoader::class.java.protectionDomain.codeSource.location.toURI().toPath(), toml, null, isBuiltIn = true))
            }

            // Loads gametests
            for (url in this::class.java.classLoader.getResources("META-INF/neoforge.mods.toml")) {
                if (url.file.contains(".jar") && url.file.contains("!/META-INF/neoforge.mods.toml")) // Prevent Fabric mods with broken Forge metadata from loading in the dev env
                    continue

                val path = Path(url.path.removePrefix("file:/"))

                val toml = tomlParser.parse(url)
                modsList.addAll(parseModsToml(path, toml, null, isBuiltIn = true))
            }

            modsList
        } else {
            val nytheriaCompatFile = NytheriaCompatLoader::class.java.protectionDomain.codeSource.location.toURI().toPath()

            JarFile(nytheriaCompatFile.toFile()).use { nytheriaCompatJar ->
                val toml = nytheriaCompatJar.getInputStream(nytheriaCompatJar.getJarEntry("META-INF/nytheria_compat_neoforge.mods.toml")).use { tomlParser.parse(it) }

                parseModsToml(nytheriaCompatFile, toml, null, isBuiltIn = true)
            }
        }
    }

    // Copied from HashMap::hash
    fun hash(key: Any?): Int {
        val h: Int
        return if (key == null) 0 else (key.hashCode().also { h = it }) xor (h ushr 16)
    }

    /**
     * NeoForge mods are sorted based on the order of keys in a hash map.
     * See: https://github.com/neoforged/FancyModLoader/blob/1.21.1/loader/src/main/java/net/neoforged/fml/loading/UniqueModListBuilder.java#L43
     * This won't sort the mods in exactly the same order, but it will be close enough to hopefully resolve most issues caused by mods not explicitly denoting their dependencies.
     * Users can always explicitly override this order with dependency overrides.
     */
    fun getNeoForgeNaturalOrder(): Comparator<NeoForgeMod> {
        return Comparator.comparing { hash(it.modId) }
    }

    override fun finishModScanning() {
        val graph = this.mods.buildGraph()
        val sorted = TopologicalSort.topologicalSort(graph, getNeoForgeNaturalOrder())

        // Sort the mods, otherwise stuff breaks.
        val modsRef = this.mods as MutableList<NeoForgeMod>
        modsRef.clear()
        modsRef.addAll(sorted)

        if (this.hasMod("embeddium") || this.hasMod("rubidium")) {
            KnitLoader.instance.displayError("Nytheria Compat: You are using a Sodium fork, which is not supported under Nytheria Compat!", IllegalStateException())
        }

        // Scan all mod classes. This needs to be run early, because some Forge mods rely on scan data as early as mixin containers.
        NytheriaCompatTimings.time("scanModClasses") { scanModClasses() }

        // Load mod access transformers and enum extensions
        NytheriaCompatTimings.time("access transformer conversion") { AccessTransformerLoader.convertTransformers(mods.flatMap(::readTransformers)) }
        NytheriaCompatTimings.time("enum extension loading") { mods.forEach(EnumExtensionLoader::loadEnumExtension) }
        NytheriaCompatTimings.time("applyEnumExtensions") { EnumExtensionLoader.applyEnumExtensions() }

        // Nytheria Compat: on a start that remapped something, the mappings have done their work now.
        NytheriaCompatRemapper.release()
    }

    override suspend fun createModContainers(definitions: Collection<ModDefinition>): Collection<NeoForgeMod> {
        // First we want to set up Union.
        try {
            // Nytheria Compat: the helper adds Union to the JDK's list of file system providers, which is null until this first call.
            FileSystemProvider.installedProviders()
            KiltUnionFileSystemHelper.directlyLoadIntoClassLoader(FabricLauncherBase.getLauncher().targetClassLoader)
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }

        val remappedModsDir = (nytheriaCompatCacheDir / "remappedMods").apply {
            runCatching { createDirectories() }
        }

        // Remaps all Forge mods from SRG to Intermediary/Yarn/MojMap
        try {
            NytheriaCompatTimings.time("remapMods") { NytheriaCompatRemapper.remapMods(definitions, remappedModsDir) }
        } catch (e: Throwable) {
            e.printStackTrace()
            throw RuntimeException("Errors occurred while remapping NeoForge mods!", e)
        }

        val mods = mutableListOf<NeoForgeMod>()

        // Then creates the mod containers for each mod.
        for (definition in definitions) {
            loadedModIds.add(definition.id)

            val config = definition.additionalData["config"] as NightConfigWrapper

            mods.add(NeoForgeMod(definition,
                showAsResourcePack = config.getConfigElement<Boolean>("showAsResourcePack").orElse(false),
                modConfig = config,
                modFile = definition.path.run {
                    if (this.extension == "jar")
                        this.toFile()
                    else null // If this is a built-in, let's not actually bother with it
                }
            ))
        }

        return mods
    }

    override fun preInitialize() {
        // DON'T TRY TO MAKE THIS USE "Environment.Keys".
        // OTHERWISE THE BUILD WILL FAIL.
        environment.computePropertyIfAbsent(IEnvironment.buildKey("FORGEDIST", Dist::class.java).get()) { DistUtil.envTypeToDist(FabricLoader.getInstance().environmentType) }
        //environment.computePropertyIfAbsent(IEnvironment.buildKey("MODFILEFACTORY", ModFileFactory::class.java).get()) { NytheriaCompatModFileFactory() }

        environment.computePropertyIfAbsent(IEnvironment.Keys.VERSION.get()) { MC_VERSION.friendlyString }
        Launcher.INSTANCE.environment().computePropertyIfAbsent(IEnvironment.Keys.VERSION.get()) { MC_VERSION.friendlyString }
        environment.computePropertyIfAbsent(IEnvironment.Keys.GAMEDIR.get()) { FabricLoader.getInstance().gameDir }
        environment.computePropertyIfAbsent(IEnvironment.Keys.ASSETSDIR.get()) { Path(FabricLoaderImpl.INSTANCE.gameProvider.arguments.getOrDefault("assetsDir", FabricLoader.getInstance().gameDir.absolutePathString())) }
        environment.computePropertyIfAbsent(IEnvironment.Keys.LAUNCHTARGET.get()) { FabricLoader.getInstance().environmentType.name.lowercase() }
        environment.computePropertyIfAbsent(IEnvironment.Keys.UUID.get()) { FabricLoaderImpl.INSTANCE.gameProvider.arguments.getOrDefault("uuid", "00000000-00000000-00000000-00000000") }
        Environment.build(environment) // Use Nytheria Compat's environment
        FMLPaths.setup(environment) // jesus christ

        // Load all of the Forge access transformers
        NytheriaCompatTimings.time("AccessTransformerLoader.runTransformers") { AccessTransformerLoader.runTransformers() }
    }

    fun scanModClasses() {
        NytheriaCompat.logger.info("Scanning all NeoForge mod classes...")

        val exception = RuntimeException("Failed to scan NeoForge mod classes in Nytheria Compat!")

        runBlocking {
            launch(Dispatchers.Default) {
                // Nytheria Compat: the translator's own scan data is only used by built-in mods without a mod file (the development
                //  environment), so only scan the translator's classes when such a mod exists.
                val forgeScanData by lazy {
                    val scanData = ModFileScanData()

                    NytheriaCompatTimings.time("scanning translator classes") {
                        NytheriaCompatHelper.forEachForgeClass { classNode ->
                            val visitor = ModClassVisitor()
                            classNode.accept(visitor)

                            visitor.buildData(scanData.classes, scanData.annotations)
                        }
                    }

                    scanData
                }

                // TODO: Need to make sure to group mods together so they load in the correct order from each other
                mods.asFlow().concurrent()
                    .collect { mod ->
                        if (mod.modFile == null) { // This is usually a Forge built-in, we don't have to worry about scanning this.
                            // If it is in fact a built-in, let's assign the scan data.
                            if (mod.definition.isBuiltin) {
                                forgeScanData.addModFileInfo(ModFileInfo(mod))
                                mod.scanData.`nytheriacompat$copyFrom`(forgeScanData)
                            }

                            return@collect
                        }

                        mod.scanData.addModFileInfo(ModFileInfo(mod))

                        try {
                            scanModJar(mod)
                        } catch (e: Exception) {
                            synchronized(exception) {
                                exception.addSuppressed(RuntimeException("Failed to scan ${mod.modId}", e))
                            }
                        }
                    }
            }.join()
        }

        NytheriaCompatScanCache.removeUnusedEntries(mods.mapNotNull { it.modFile?.toPath() })

        if (exception.suppressed.isNotEmpty()) {
            exception.printStackTrace()
            KnitLoader.instance.displayError("Errors occurred while scanning NeoForge mod classes!", exception)
        }
    }

    private suspend fun scanModJar(mod: NeoForgeMod) {
        val scanStart = System.nanoTime()
        val jarPath = mod.modFile!!.toPath()

        // Nytheria Compat: reuse the scan data of an unchanged jar, see NytheriaCompatScanCache.
        val cached = NytheriaCompatScanCache.read(jarPath)
        if (cached != null) {
            mod.scanData.classes.addAll(cached.classes)
            mod.scanData.annotations.addAll(cached.annotations)
            NytheriaCompatTimings.log("class scan of ${mod.modId} (cached)", System.nanoTime() - scanStart)
            return
        }

        val classes = ConcurrentHashMap.newKeySet<ModFileScanData.ClassData>()
        val annotations = ConcurrentHashMap.newKeySet<ModFileScanData.AnnotationData>()

        // basically emulate how Forge loads stuff
        // Nytheria Compat: NeoForgeMod.jar opens a new JarFile on every access, so open it once for the whole scan.
        withContext(Dispatchers.IO) { mod.jar }.use { jar ->
            jar.stream().consumeAsFlow().concurrent()
                .filter { it.name.endsWith(".class") }
                .collect { entry ->
                    val visitor = ModClassVisitor()
                    val classReader = withContext(Dispatchers.IO) { jar.getInputStream(entry) }.use { ClassReader(it) }

                    classReader.accept(visitor, 0)
                    visitor.buildData(classes, annotations)
                }
        }

        // This needs to be sorted, otherwise there is a very high possibility of packet desync between client-server,
        // because for whatever reason Forge uses int packet IDs and MCreator mods don't register packets in one place.
        val sortedClasses = classes.sortedWith { a, b -> a.clazz.className.compareTo(b.clazz.className) }
        val sortedAnnotations = annotations.sortedWith { a, b -> a.clazz.className.compareTo(b.clazz.className) }
        mod.scanData.classes.addAll(sortedClasses)
        mod.scanData.annotations.addAll(sortedAnnotations)
        NytheriaCompatTimings.log("class scan of ${mod.modId}", System.nanoTime() - scanStart)

        NytheriaCompatScanCache.write(jarPath, sortedClasses, sortedAnnotations)
    }

    fun loadMods() = NytheriaCompatTimings.time("constructing NeoForge mods") { loadModsInternal() }

    private fun loadModsInternal() {
        NytheriaCompat.logger.info("Starting initialization of NeoForge mods...")

        // Needed by Twilight Forest
        FMLLoader.progressWindowTick = {}
        FMLLoader.beforeStart(ModuleLayer.boot())

        val exception = RuntimeException("Failed to load NeoForge mods in Nytheria Compat!")

        // Create all mod containers
        val languageLoaders = this.languageLoaders

        runBlocking {
            val modsByLoaders = mods.groupBy { mod ->
                languageLoaders.firstOrNull { loader ->
                    loader.name() == mod.loader
                } ?: run {
                    exception.addSuppressed(IllegalArgumentException("No language loader found by ID ${mod.loader}!"))
                    NoopLanguageLoader
                }
            }

            if (exception.suppressed.isNotEmpty()) {
                exception.printStackTrace()
                KnitLoader.instance.displayError("Errors occurred while creating NeoForge mods!", exception)
            }

            modsByLoaders.entries.asFlow()
                .collect { (loader, mods) ->
                    mods.asFlow().concurrent().collect { mod ->
                        mod.container = loader.loadMod(mod, mod.scanData, ModuleLayer.empty())
                    }
                }
        }

        // Actual initializing goes into FML now, yay

        if (exception.suppressed.isNotEmpty()) {
            exception.printStackTrace()
            KnitLoader.instance.displayError("Errors occurred while loading NeoForge mods!", exception)
        }
    }

    private fun readTransformers(mod: NeoForgeMod): List<ByteArray> {
        // Built-in mods have no jar of their own, Nytheria Compat's own access changes are in nytheria_compat.classtweaker.
        if (mod.modFile == null || mod.definition.isBuiltin)
            return emptyList()

        val declared = (mod.definition.additionalData["accessTransformers"] as? List<*>).orEmpty().filterIsInstance<String>()

        return mod.jar.use { jar ->
            ModJars.readAccessTransformers(jar, declared) { path ->
                NytheriaCompat.logger.warn("Could not find explicitly-specified access transformer $path for ${mod.modId}")
            }
        }
    }

    // ImmediateWindowHandler reads this. None of the four mods asks for a newer OpenGL version.
    fun getOpenGlVersionString(): String = "3.2"

    fun postEvent(ev: Event) {
        mods.forEach {
            it.eventBus.post(ev)
        }
    }

    fun getMod(id: String): NeoForgeMod? {
        return mods.firstOrNull { it != null && it.modId == id }
    }

    fun hasMod(id: String): Boolean {
        return this.loadedModIds.contains(id)
    }

    companion object {
        val instance: NytheriaCompatLoader
            get() = KnitLoader.instance.getLoaderById("nytheria_compat") as NytheriaCompatLoader

        const val NYTHERIACOMPAT_ERROR_MESSAGE = "Nytheria Compat: Failed to start Nytheria Compat, please read the exception below!"
        private const val NEOFORGE_MOD_ID = "neoforge"

        // These constants are to be updated each time we change versions
        val SUPPORTED_FML_VERSION = Constants.NEOFORGE_LOADER_VERSION
        val SUPPORTED_NEO_API_VERSION = Constants.NEOFORGE_API_VERSION
        val MC_VERSION = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().metadata.version

        private val MOD_ANNOTATION = Type.getType(Mod::class.java)

        val nytheriaCompatCacheDir = (FabricLoader.getInstance().gameDir / ".nytheria_compat").apply {
            runCatching { this.createDirectories() }
        }
        private val extractedModsDir = (nytheriaCompatCacheDir / "extractedMods").apply {
            runCatching { this.createDirectories() }
        }
    }
}
