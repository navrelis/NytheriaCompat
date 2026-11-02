/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.stream.consumeAsFlow
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.MappingResolver
import net.fabricmc.loader.impl.game.GameProviderHelper
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.loader.impl.launch.MappingConfiguration
import net.fabricmc.loader.impl.util.SystemProperties
import net.fabricmc.mapping.tree.TinyMappingFactory
import net.minecraftforge.fart.api.ClassProvider
import net.minecraftforge.fart.internal.EnhancedClassRemapper
import net.minecraftforge.fart.internal.EnhancedRemapper
import net.minecraftforge.fart.internal.RenamingTransformer
import net.minecraftforge.srgutils.IMappingFile
import net.minecraftforge.srgutils.INamedMappingFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory
import dev.nytheria.compat.NytheriaCompat
import dev.nytheria.compat.loader.NytheriaCompatFlags
import dev.nytheria.compat.loader.NytheriaCompatLoader
import dev.nytheria.compat.loader.remap.fixers.*
import dev.nytheria.compat.loader.remap.fixers.mixin.*
import dev.nytheria.compat.loader.remap.resource.IgnoreSignatureResourceRemapper
import dev.nytheria.compat.loader.remap.resource.ManifestResourceRemapper
import dev.nytheria.compat.util.CaseInsensitiveStringHashSet
import dev.nytheria.compat.util.ClassNameHashSet
import dev.nytheria.compat.util.NytheriaCompatHelper
import dev.nytheria.compat.util.NytheriaCompatTimings
import xyz.bluspring.knit.loader.mod.ModDefinition
import xyz.bluspring.knit.loader.util.collect
import xyz.bluspring.knit.loader.util.concurrent
import xyz.bluspring.knit.loader.util.launchIn
import xyz.bluspring.knit.loader.util.onEach
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.function.Consumer
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.io.path.*
import kotlin.time.measureTime

object NytheriaCompatRemapper {
    // Keeps track of the remapper changes, so every time I update the remapper,
    // it remaps all the mods following the remapper changes.
    // this can update by like 12 versions in 1 update, so don't worry too much about it.
    const val REMAPPER_VERSION = 257
    const val MC_MAPPED_JAR_VERSION = 10

    // Nytheria Compat JVM flags
    private val forceRemap = NytheriaCompatFlags.FORCE_REMAPPING
    private val disableRemaps = NytheriaCompatFlags.DISABLE_REMAPPING
    internal val forceProductionRemap = NytheriaCompatFlags.FORCE_PRODUCTION_REMAPPING

    // GSON stuff for pretty printing
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    val logConsumer = Consumer<String> {
        logger.debug(it)
    }

    internal val logger = LoggerFactory.getLogger("Nytheria Compat Remapper")
    // internal val useNamed = FabricLoader.getInstance().mappingResolver.currentRuntimeNamespace != "intermediary"

    // Remapper extensions
    fun MappingResolver.mapClass(clazz: Class<*>): String = mapClassName("intermediary", "net.minecraft.$clazz").replace(".", "/")

    private val mappingResolver = if (forceProductionRemap)
        NoopMappingResolver()
    else
        FabricLoader.getInstance().mappingResolver

    // Nytheria Compat: in production the game runs on intermediary, so the runtime names are the intermediary names.
    private val runsOnIntermediary = forceProductionRemap || FabricLoader.getInstance().mappingResolver.currentRuntimeNamespace == "intermediary"

    // Nytheria Compat: only the development environment needs Fabric's own mappings, so don't parse them on every production start.
    val fabricMappings: INamedMappingFile by lazy { MappingConfiguration::class.java.classLoader.getResourceAsStream("mappings/mappings.tiny")!!.use { INamedMappingFile.load(it) } }

    // Nytheria Compat: moj -> intermediary class names, see ClassNameTable.
    private val classNames by lazy {
        NytheriaCompatTimings.time("loading moj_intermediary class names") {
            this::class.java.getResourceAsStream("/moj_intermediary.tiny")!!.bufferedReader().useLines { ClassNameTable(it) }
        }
    }

    // Some workaround mappings to remap some names to Nytheria Compat equivalents.
    // This fixes some compatibility issues.
    private val workaroundClasses by lazy {
        this::class.java.getResourceAsStream("/nytheria_compat_workaround_mappings.tiny")!!.bufferedReader().use { reader ->
            TinyMappingFactory.load(reader).classes.associate { it.getRawName("forge") to it.getRawName("nytheria_compat") }
        }
    }

    /**
     * Nytheria Compat: the full member mappings and what is built from them. A start on a full remap cache never gets here: see [remapMods]
     * and AccessTransformerLoader, which replays its result. [release] drops all of this again after a remap.
     */
    private class Mappings {
        // This is created automatically using https://github.com/BluSpring/srg2intermediary
        // moj -> intermediary
        val moj: IMappingFile = NytheriaCompatTimings.time("loading moj_intermediary.tiny") { NytheriaCompatRemapper::class.java.getResourceAsStream("/moj_intermediary.tiny")!!.buffered().use { IMappingFile.load(it) } }
        val runtime: IMappingFile = if (runsOnIntermediary) moj else moj.rename(DevMappingRenamer(FabricLoader.getInstance().mappingResolver))
        val runtimeReversed: IMappingFile = runtime.reverse()

        // Moj name -> (parent class name, intermediary/mapped name)
        val fields = HashMap<String, MutableMap<String, String>>()

        // Moj name -> (parent class name, (intermediary/mapped name, descriptor))
        val methods = HashMap<String, MutableMap<String, MutableSet<Pair<String, String>>>>()

        init {
            NytheriaCompatTimings.time("moj -> runtime member table") {
                for (cls in runtime.classes) {
                    for (f in cls.fields) {
                        val mapped = if (runsOnIntermediary) f.mapped else mappingResolver.mapFieldName("intermediary", cls.mapped.replace("/", "."), f.mapped, f.mappedDescriptor)
                        fields.getOrPut(f.original) { HashMap() }[f.parent.original] = mapped
                    }

                    for (m in cls.methods) {
                        val mapped = if (runsOnIntermediary) m.mapped else mappingResolver.mapMethodName("intermediary", cls.mapped.replace("/", "."), m.mapped, m.mappedDescriptor)
                        methods.getOrPut(m.original) { HashMap() }.getOrPut(m.parent.original) { LinkedHashSet() }.add(mapped to m.mappedDescriptor)
                    }
                }
            }
        }
    }

    @Volatile
    private var mappingsHolder: Mappings? = null

    private fun mappings(): Mappings = mappingsHolder ?: synchronized(this) {
        mappingsHolder ?: Mappings().also { mappingsHolder = it }
    }

    val mojIntermediaryMapping: IMappingFile
        get() = mappings().moj
    val mojMappedFields: MutableMap<String, MutableMap<String, String>>
        get() = mappings().fields
    val mojMappedMethods: MutableMap<String, MutableMap<String, MutableSet<Pair<String, String>>>>
        get() = mappings().methods

    private class Remappers(val forward: NytheriaCompatEnhancedRemapper, val inverse: NytheriaCompatEnhancedRemapper)

    @Volatile
    private var remappersHolder: Remappers? = null

    val enhancedRemapper: NytheriaCompatEnhancedRemapper
        get() = remappers().forward
    val enhancedInverseRemapper: NytheriaCompatEnhancedRemapper
        get() = remappers().inverse

    private lateinit var remappedModsDir: Path

    // The jars of all NeoForge mods as they were before remapping: a class provider has to see the Moj names.
    private var modLibraries: List<Path> = emptyList()

    /**
     * Nytheria Compat: drops the mappings and remappers, they are built again if anything still asks. Called once loading has no more use for them.
     */
    fun release() {
        synchronized(this) {
            mappingsHolder = null
            remappersHolder = null
        }
    }

    private fun initEnhancedRemapper(intermediaryMap: Path?): ClassProvider {
        // Initialize this a bit later, cuz we need the same libraries.
        return ClassProvider.builder().apply {
            // time to add Intermediary mappings to the mix! :,D
            if (FabricLoader.getInstance().isDevelopmentEnvironment && !forceProductionRemap) {
                addLibrary(intermediaryMap)
            }

            // IMPORTANT: this cannot be a flow or use merge, otherwise the order isn't retained. mojGamePath MUST be at the top of the list.
            listOf(
                // List down NeoForge paths
                *NytheriaCompatHelper.getNytheriaCompatPaths().toTypedArray(),
                // Add all Fabric mods
                *FabricLoader.getInstance().allMods
                    .flatMap { container -> container.rootPaths }.toTypedArray(),
                // add mapped path too
                *runBlocking { getGameClassPath() },
                // Add all NeoForge mods to the library path, because dependencies don't have to be specified
                // in order to use mods lmao
                *modLibraries.toTypedArray()
            ).forEach {
                addLibrary(it)
            }
        }.build()
    }

    private fun remappers(): Remappers = remappersHolder ?: synchronized(this) {
        remappersHolder ?: runBlocking { createRemappers() }.also { remappersHolder = it }
    }

    private suspend fun createRemappers(): Remappers {
        val mappings = mappings()
        val mojGamePath = NytheriaCompatTimings.time("remapMinecraft") { remapMinecraft("mojang", mappings.runtimeReversed) }

        // Use the regular mod file
        val classProvider = NytheriaCompatTimings.time("building the remapper class provider") { ClassProvider.builder().apply {
            // IMPORTANT: this cannot be a flow or use merge, otherwise the order isn't retained. mojGamePath MUST be at the top of the list.
            listOf(
                mojGamePath,
                // List down NeoForge paths
                *NytheriaCompatHelper.getNytheriaCompatPaths().toTypedArray(),
                // Add all Fabric mods
                *FabricLoader.getInstance().allMods
                    .flatMap { container -> container.rootPaths }.toTypedArray(),
                // add mapped path too
                *getGameClassPath(),
                // Add all NeoForge mods to the library path, because dependencies don't have to be specified
                // in order to use mods lmao
                *modLibraries.toTypedArray()
            ).forEach {
                addLibrary(it)
            }
        }.build() }

        val intermediaryMap = if (FabricLoader.getInstance().isDevelopmentEnvironment && !forceProductionRemap)
            remapMinecraft("intermediary", fabricMappings.getMap("named", "intermediary").rename(DevMojClassMappingRenamer(FabricLoader.getInstance().mappingResolver)))
        else null

        // Initialize a global remapper state
        val forward = NytheriaCompatEnhancedRemapper(classProvider, mappings.runtime, logConsumer) { initEnhancedRemapper(intermediaryMap) }
        val inverse = NytheriaCompatEnhancedRemapper(classProvider, mappings.runtimeReversed, logConsumer) { initEnhancedRemapper(intermediaryMap) }

        if (FabricLoader.getInstance().isDevelopmentEnvironment) {
            forward.initDevRemapper()
            inverse.initDevRemapper()
        }

        return Remappers(forward, inverse)
    }

    /**
     * Nytheria Compat: a remapped jar is known by the size and modification time of its mod jar. A file name is either exactly what the
     * current mods expect or stale, so no mod ID has to be parsed out of it (`foo` used to claim the files of `foo_bar`).
     */
    private fun remappedJarName(mod: ModDefinition): String {
        return "${mod.id}_${REMAPPER_VERSION}_${mod.path.fileSize()}_${mod.path.getLastModifiedTime().toMillis()}.jar"
    }

    private fun deleteStaleJars(expectedNames: Set<String>) {
        NytheriaCompatTimings.time("stale remap cache sweep") {
            val minecraftPrefix = "minecraft_${NytheriaCompatLoader.MC_VERSION.friendlyString}-"
            for (file in NytheriaCompatLoader.nytheriaCompatCacheDir.listDirectoryEntries("minecraft_*.jar")) {
                if (!file.name.startsWith(minecraftPrefix) || !file.nameWithoutExtension.endsWith("_$MC_MAPPED_JAR_VERSION"))
                    file.deleteIfExists()
            }

            for (file in remappedModsDir.listDirectoryEntries()) {
                if ((file.extension == "jar" || file.extension == "tmp") && file.name !in expectedNames)
                    file.deleteIfExists()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun remapMods(modLoadingQueue: Collection<ModDefinition>, remappedModsDir: Path) {
        if (disableRemaps) {
            logger.warn("Mod remapping has been disabled! Mods built normally using ForgeGradle will not function with this enabled.")
            logger.warn("Only have this enabled if you know what you're doing!")

            return
        }

        this.remappedModsDir = remappedModsDir
        modLibraries = modLoadingQueue.map { it.path }

        if (forceRemap)
            logger.warn("Forced remaps enabled! All NeoForge mods will be remapped.")

        val targets = modLoadingQueue.filter { !it.isBuiltin }.map { it to remappedModsDir / remappedJarName(it) }
        val pending = targets.filter { (mod, target) ->
            if (target.exists() && !forceRemap) {
                mod.path = target
                false
            } else true
        }

        if (pending.isEmpty())
            return

        deleteStaleJars(targets.map { it.second.name }.toSet())

        // Forcefully initialize the mapping resolver.
        // Otherwise, it just crashes from trying to initialize concurrently.
        mappingResolver.unmapClassName("intermediary", "net.minecraft.class_1937")

        val exception = RuntimeException("Failed to remap NeoForge mods in Nytheria Compat!")

        logger.info("Remapping NeoForge mods...")

        val enhancedRemapper = enhancedRemapper

        //val mixinRemapper = NytheriaCompatMixinRemapper(enhancedRemapper, mojIntermediaryMapping, classProvider)
        val resourceRemappers = listOf(
            ManifestResourceRemapper,
            IgnoreSignatureResourceRemapper
        )

        suspend fun remapMod(file: Path, mod: ModDefinition, modifiedJarFile: Path) {
            val exception = RuntimeException("Failed to remap NeoForge mod ${mod.displayName} (${mod.id})!")

            // Make a temporary file, so if any errors occur in remapping, we don't end up with a broken state.
            val tempModifiedJarFile = modifiedJarFile.resolveSibling("${modifiedJarFile.name}.tmp")
            val jar = withContext(Dispatchers.IO) { JarFile(file.toFile()) }
            val jarOutputStream = withContext(Dispatchers.IO) { JarOutputStream(tempModifiedJarFile.outputStream(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) }

            val entryToClassNodes = Object2ReferenceMaps.synchronize(Object2ReferenceOpenHashMap<JarEntry, ClassNode>())
            // val classesToProcess = entryToClassNodes.values.intersect(NytheriaCompatHelper.getForgeClassNodes().toSet()).toList()

            val mixinClasses = Collections.synchronizedSet(ClassNameHashSet())
            val refmaps = Collections.synchronizedSet(CaseInsensitiveStringHashSet())
            val mixinRefmaps = Collections.synchronizedMap<JarEntry, MixinRefmap>(mutableMapOf())

            // JAR validation information stripping.
            // If we can find out how to use this to our advantage prior to remapping,
            // we may still be able to use this information safely.
            val manifestEntry = jar.getJarEntry("META-INF/MANIFEST.MF")
            if (manifestEntry != null) {
                val manifest = jar.getInputStream(manifestEntry).use { Manifest(it) }
                val mixinConfigs = manifest.mainAttributes.getValue("MixinConfigs")?.split(",")?.toMutableSet() ?: mutableSetOf()

                // Search for more mixin configs, because apparently it's possible to define with a fucking class.
                for (entry in jar.stream()) {
                    if (entry.name.endsWith(".mixin.json") || entry.name.endsWith(".mixins.json")) {
                        mixinConfigs.add(entry.name)
                    } else if (entry.name.endsWith(".refmap.json")) {
                        // Search for more refmaps too, because they're probably defined in class too.
                        refmaps.add(entry.name)
                    }
                }

                // Read mixin configs and add them to the list of mixins to fix
                mixinConfigs.asFlow().collect { config ->
                    val jsonEntry = jar.getJarEntry(config) ?: return@collect
                    val json = jar.getInputStream(jsonEntry).use {
                        JsonParser.parseReader(it.reader())
                    }.asJsonObject

                    if (!json.has("package")) return@collect

                    val mixinPackage = json.get("package").asString

                    merge(
                        (json.get("mixins") as? JsonArray)?.asFlow() ?: emptyFlow(),
                        (json.get("client") as? JsonArray)?.asFlow() ?: emptyFlow(),
                        (json.get("server") as? JsonArray)?.asFlow() ?: emptyFlow()
                    ).collect {
                        if (!it.isJsonNull)
                            mixinClasses.add("$mixinPackage.${it.asString}")
                    }

                    runCatching { json.get("refmap")!!.asString }.onSuccess { refmaps.add(it) }
                }
            }

            withContext(Dispatchers.IO) {
                val seen = HashSet<String>()
                jar.stream().consumeAsFlow().collect { entry ->
                    // Ars Nouveau's jar has duplicate entries. wtf?????
                    if (!seen.add(entry.name)) {
                        logger.warn("Mod ${mod.displayName} (${mod.id})'s jar file is malformed; saw duplicate of ${entry.name}!")
                        return@collect
                    }
                    // Transform some specific files
                    for (remapper in resourceRemappers) {
                        if (remapper.canTransform(entry.name)) {
                            val data = jar.getInputStream(entry).use { remapper.transform(entry.name, it) }

                            if (data != null) {
                                jarOutputStream.putNextEntry(entry)
                                jarOutputStream.write(data)
                                jarOutputStream.closeEntry()
                            }

                            return@collect
                        }
                    }

                    // Store refmap JSONs
                    if (refmaps.contains(entry.name)) {
                        val json = jar.getInputStream(entry).use { it.reader(Charsets.UTF_8).use { r -> JsonParser.parseReader(r) } }

                        if (json.isJsonObject) {
                            val obj = json.asJsonObject

                            // Be careful, because they might not even have the data in the first place.
                            if (obj.has("mappings")) {
                                mixinRefmaps[entry] = MixinRefmap(
                                    Collections.synchronizedMap(
                                        obj.getAsJsonObject("mappings").asMap()
                                            .map {
                                                it.key to it.value.asJsonObject.asMap().map { b ->
                                                    b.key to b.value.asString
                                                }
                                                    .associate { b -> b.first to b.second }
                                                    .toMutableMap()
                                            }
                                            .associate { it.first to it.second }
                                            .toMutableMap()
                                    ),
                                    Collections.synchronizedMap(mutableMapOf())
                                )
                            }
                        }

                        return@collect
                    }

                    if (entry.name.endsWith(".class")) {
                        // Add the class files for remapping
                        val classReader = jar.getInputStream(entry).use { ClassReader(it) }

                        // we need the info for this for the class writer
                        val classNode = ClassNode(Opcodes.ASM9)
                        classReader.accept(classNode, 0)

                        entryToClassNodes[entry] = classNode
                    } else {
                        // Otherwise, add the entry directly if everything's already been processed.
                        jarOutputStream.putNextEntry(entry)
                        jar.getInputStream(entry).use { it.copyTo(jarOutputStream) }
                        jarOutputStream.closeEntry()
                    }
                }
            }

            fun isMixinClass(node: ClassNode): Boolean {
                return node.name in mixinClasses ||
                    // GUESS WHAT, SOME MODS DON'T FUCKING DEFINE SOME MIXINS IN THE FILE, INSTEAD IN THE MIXIN PLUGIN.
                    // SO LET'S JUST RUN THIS ON EVERYTHING THAT HAS THE BLOODY ANNOTATION.
                    NytheriaCompatHelper.mergeNullableCollections(node.visibleAnnotations, node.invisibleAnnotations)
                        .any { it.desc == MixinTypes.MIXIN.descriptor }
            }

            // Make copies of the original ClassNode objects so we can use them as reference to remap inherited shadows.
            val originalMappings = mutableMapOf<String, ClassNode>()
            for (node in entryToClassNodes.values) {
                if (isMixinClass(node)) {
                    val writer = ClassWriter(0)
                    node.accept(writer)
                    val nodeCopy = ClassReader(writer.toByteArray())
                    val newNode = ClassNode()
                    nodeCopy.accept(newNode, 0)
                    originalMappings[node.name] = newNode
                }
            }

            val throwable = entryToClassNodes.entries.asFlow().concurrent().runCatching {
                this.collect { (entry, originalNode) ->
                    try {
                        val remappedNode = ClassNode(Opcodes.ASM9)

                        // only do this on mixin classes, please
                        // We must remap the mixins before actually remapping them to Intermediary, so the names are correct in prod.
                        if (isMixinClass(originalNode)) {
                            MixinRemapper.remapClass(originalNode, enhancedRemapper, mixinRefmaps.values)
                            MixinShadowRemapper.remapClass(originalNode, enhancedRemapper, originalMappings)

                            MixinAdditionalRemapper.remapClass(originalNode)
                            MixinStaticMethodFixer.fixClass(originalNode)
                            MixinDirectModifierFixer.fixClass(originalNode)
                        }

                        originalNode.accept(EnhancedClassRemapper(remappedNode, enhancedRemapper, RenamingTransformer(enhancedRemapper, false)))

                        ConditionalInterfaceInjectionFixer.fixClass(remappedNode)
                        EventClassVisibilityFixer.fixClass(remappedNode)
                        InjectedInterfaceVisibilityFixer.fixClass(remappedNode)
                        ObjectHolderDefinalizer.processClass(remappedNode)
                        WorkaroundFixer.fixClass(remappedNode)
                        ConflictingStaticMethodFixer.fixClass(remappedNode)
                        EnvironmentRemapper.remapClass(remappedNode)
                        EnvironmentLambdaFixer.fixClass(remappedNode)

                        val writer = ClassWriter(Opcodes.ASM9)
                        remappedNode.accept(writer)

                        synchronized(jarOutputStream) {
                            jarOutputStream.putNextEntry(entry)
                            jarOutputStream.write(writer.toByteArray())
                            jarOutputStream.closeEntry()
                        }
                    } catch (e: Throwable) {
                        throw RuntimeException("Failed to remap ${entry.name}", e)
                    }
                }
            }.exceptionOrNull()

            if (throwable != null) {
                exception.addSuppressed(throwable)
            }
            
            // If for whatever reason the refmap remapping missed something, we need to remap it immediately.
            MixinRemapper.remapUnmappedRefmaps(mixinRefmaps.values, enhancedRemapper)

            // Now, let's write the refmap JSONs
            for ((entry, refmap) in mixinRefmaps) {
                val json = JsonObject()

                // First, write the mappings directly.
                val mappings = JsonObject()
                for ((mixinClass, nameMappings) in refmap.mappings) {
                    val classMapping = JsonObject()
                    for ((named, obfuscated) in nameMappings) {
                        classMapping.addProperty(named, obfuscated)
                    }

                    mappings.add(mixinClass, classMapping)
                }
                json.add("mappings", mappings)

                // Now, let's add the mapping data map directly to "named:intermediary"
                val dataMap = JsonObject()
                dataMap.add("named:intermediary", mappings)

                json.add("data", dataMap)

                jarOutputStream.putNextEntry(entry)
                jarOutputStream.write(gson.toJson(json).toByteArray(Charsets.UTF_8))
                jarOutputStream.closeEntry()
            }


            jarOutputStream.close()
            jar.close()

            if (exception.suppressed.isNotEmpty())
                throw exception

            // We've finished writing the mod file successfully, so let's not make it temporary anymore.
            tempModifiedJarFile.moveTo(modifiedJarFile, true)
            mod.path = modifiedJarFile
        }

        coroutineScope {
            pending.asFlow().concurrent()
                .onEach { (mod, target) ->
                    runCatching {
                        logger.info("Remapping ${mod.displayName} (${mod.id})")
                        val ms = measureTime {
                            remapMod(mod.path, mod, target)
                        }.inWholeMilliseconds
                        logger.info("Remapped ${mod.displayName} (${mod.id}) [took ${ms}ms]")
                    }.onFailure {
                        logger.error("Failed to remap ${mod.displayName} (${mod.id})", it)
                        if (it is Exception) {
                            exception.addSuppressed(it)
                        }
                    }
                }.launchIn(this).join()
        }

        // Clear any caches stored in the fixers
        MixinRemapper.clearCache()
        enhancedRemapper.clearCaches()
        enhancedInverseRemapper.clearCaches()

        logger.info("Finished remapping mods!")

        if (exception.suppressed.isNotEmpty()) {
            logger.error("Ran into some errors during the remapping process, cannot continue!")
            throw exception
        }
    }

    @JvmOverloads
    fun remapClass(name: String, toIntermediary: Boolean = false, ignoreWorkaround: Boolean = false): String {
        val workaround = if (!ignoreWorkaround) workaroundClasses[name] else null
        val intermediary = classNames.map(name.replace(".", "/"))
        if (toIntermediary) {
            return workaround ?: intermediary
        }

        return (workaround ?: mappingResolver.mapClassName("intermediary", intermediary.replace("/", ".")) ?: name).replace(".", "/")
    }

    /**
     * Converts Intermediary mappings to Moj mappings.
     */
    fun unmapClass(name: String): String {
        val intermediary = mappingResolver.unmapClassName("intermediary", name.replace("/", "."))
        return classNames.unmap(intermediary.replace(".", "/"))
    }

    val gameFile by lazy { getMCGameFile() }


    private fun getDeobfJarDir(gameDir: Path, gameId: String, gameVersion: String): Path {
        return GameProviderHelper::class.java
            .getDeclaredMethod("getDeobfJarDir", Path::class.java, String::class.java, String::class.java)
            .apply {
                isAccessible = true
            }
            .invoke(null, gameDir, gameId, gameVersion) as Path
    }

    private fun getMCGameFile(): Path? {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment) {
            // broken on servers.
            /*val commonJar = GameProviderHelper.getCommonGameJar()

            if (commonJar != null)
                return commonJar

            val sidedJar = GameProviderHelper.getEnvGameJar(FabricLoader.getInstance().environmentType)

            if (sidedJar != null)
                return sidedJar*/

            // this gives the obfuscated JAR; we don't want that
            //val inputGameJar = FabricLoader.getInstance().objectShare.get("fabric-loader:inputGameJar")
            //if (inputGameJar is Path)
            //return inputGameJar.toFile()

            // This is our best bet towards getting the Intermediary JAR.
            val deobfJar =
                getDeobfJarDir(
                    FabricLoader.getInstance().gameDir,
                    "minecraft",
                    NytheriaCompatLoader.MC_VERSION.friendlyString
                ) / "${FabricLoader.getInstance().environmentType.name.lowercase()}-${FabricLoader.getInstance().mappingResolver.currentRuntimeNamespace}.jar"

            if (deobfJar.exists())
                return deobfJar
        } else {
            // TODO: is there a better way of doing this?
            val possibleMcGameJar = FabricLauncherBase.getLauncher().classPath.firstOrNull { path ->
                val str = path.absolutePathString()
                str.contains("net") && str.contains("minecraft") && str.contains("-loom.mappings.") && str.contains("minecraft-merged-")
            }

            return possibleMcGameJar
        }

        return null
    }

    suspend fun getGameClassPath(): Array<out Path> {
        return if (!FabricLoader.getInstance().isDevelopmentEnvironment)
            arrayOf(
                getMCGameFile()
                    ?: FabricLoader.getInstance().objectShare.get("fabric-loader:inputGameJar") as Path,
                NytheriaCompat::class.java.protectionDomain.codeSource.location.toURI().toPath()
            )
        else
            mutableListOf<Path>().apply {
                val remapClasspathFile = System.getProperty(SystemProperties.REMAP_CLASSPATH_FILE)
                    ?: throw RuntimeException("No remapClasspathFile provided")

                val content = withContext(Dispatchers.IO) { Path(remapClasspathFile).readText() }

                this.addAll(
                    content.split(File.pathSeparator.toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .map { Path(it) }
                )

                this.add(NytheriaCompat::class.java.protectionDomain.codeSource.location.toURI().toPath())
            }.toTypedArray()
    }

    private suspend fun remapMinecraft(mappingName: String, mappingFile: IMappingFile): Path {
        val mojFile =
            NytheriaCompatLoader.nytheriaCompatCacheDir / "minecraft_${NytheriaCompatLoader.MC_VERSION.friendlyString}-${mappingName}_$MC_MAPPED_JAR_VERSION.jar"

        if (mojFile.exists() && !forceRemap) {
            logger.info("${mappingName}-mapped Minecraft JAR detected, not creating a new remapped file.")
            return mojFile
        }

        val gameFile = gameFile ?: throw IllegalStateException("Minecraft JAR was not found!")

        val tempMojFile =
            NytheriaCompatLoader.nytheriaCompatCacheDir / "minecraft_${NytheriaCompatLoader.MC_VERSION.friendlyString}-${mappingName}_$MC_MAPPED_JAR_VERSION.jar.tmp"

        if (tempMojFile.exists())
            tempMojFile.deleteIfExists()

        logger.info("Creating ${mappingName}-mapped Minecraft JAR for remapping NeoForge mods...")
        val startTime = System.currentTimeMillis()

        val classProvider = ClassProvider.builder().apply {
            this.addLibrary(gameFile)
            for (path in getGameClassPath()) {
                this.addLibrary(path)
            }
        }.build()
        val mojRemapper = EnhancedRemapper(classProvider, mappingFile, logConsumer)

        val gameJar = withContext(Dispatchers.IO) { JarFile(gameFile.toFile()) }
        runCatching { tempMojFile.createFile() }

        tempMojFile.outputStream().use { outputStream ->
            withContext(Dispatchers.IO) { JarOutputStream(outputStream) }.use { outputJar ->
                // Nytheria Compat: remap the classes concurrently, like remapMod does (this was ~7 s on one thread). The entry order of
                //  this jar doesn't matter, it is only ever read as a class library through FART's ClassProvider.
                gameJar.stream().consumeAsFlow().flowOn(Dispatchers.IO).concurrent()
                    .collect { entry ->
                        if (entry.name.endsWith(".class")) {
                            val classReader = gameJar.getInputStream(entry).use { ClassReader(it) }
                            val classWriter = ClassWriter(0)

                            classReader.accept(EnhancedClassRemapper(classWriter, mojRemapper, RenamingTransformer(mojRemapper, false)), 0)

                            // We need to remap to the correct name, otherwise the remapper completely fails in production environments.
                            val mojName = mappingFile.remapClass(entry.name.removePrefix("/").removeSuffix(".class"))
                            val bytes = classWriter.toByteArray()

                            synchronized(outputJar) {
                                outputJar.putNextEntry(JarEntry("$mojName.class"))
                                outputJar.write(bytes)
                                outputJar.closeEntry()
                            }
                        } else {
                            val bytes = gameJar.getInputStream(entry).use { it.readAllBytes() }

                            synchronized(outputJar) {
                                outputJar.putNextEntry(entry)
                                outputJar.write(bytes)
                                outputJar.closeEntry()
                            }
                        }
                    }
            }
        }

        tempMojFile.moveTo(mojFile, true)

        logger.info("Remapped Minecraft to $mappingName. (took ${System.currentTimeMillis() - startTime} ms)")

        return mojFile
    }

    @JvmOverloads
    fun remapDescriptor(descriptor: String, reverse: Boolean = false, toIntermediary: Boolean = false): String {
        val formed = StringBuilder(descriptor.length + 16)
        var index = 0

        while (index < descriptor.length) {
            val char = descriptor[index]
            val end = if (char == 'L') descriptor.indexOf(';', index) else -1

            if (end == -1) {
                formed.append(char)
                index++
                continue
            }

            val name = descriptor.substring(index + 1, end)
            formed.append('L').append(if (!reverse) remapClass(name, toIntermediary) else unmapClass(name)).append(';')
            index = end + 1
        }

        return formed.toString()
    }
}
