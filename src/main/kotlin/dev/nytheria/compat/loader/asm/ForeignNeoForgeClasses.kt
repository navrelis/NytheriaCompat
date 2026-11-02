package dev.nytheria.compat.loader.asm

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import xyz.bluspring.fork.mm.api.ClassTinkerers
import dev.nytheria.compat.NytheriaCompat
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes

/**
 * Nytheria Compat: some Fabric mods bundle their own copies of NeoForge API classes under the same names Nytheria Compat ships
 * (for example NeoForge Data Pack Extensions, nested in Dyed Flames, has its own `net.neoforged.neoforge.registries.*`).
 * Knot looks classes up in the order of the mod IDs, so a mod whose ID sorts before `nytheria_compat` wins, and Nytheria Compat's own
 * NeoForge code then runs against a foreign, incompatible copy (NoSuchMethodError in `NeoForgeRegistries.<clinit>`).
 *
 * For the classes in [CLASSES], where the copy the lookup finds differs from Nytheria Compat's, the class is replaced with
 * Nytheria Compat's own bytes before any mixin is applied to it, the same way NytheriaCompatEarlyRiser already replaces Forge Config
 * API Port's config classes. The list is fixed, it is the set of classes the mods Nytheria Compat supports are known to
 * bundle; walking every Fabric mod for more costs a lot on a pack of hundreds of mods and found nothing else.
 */
object ForeignNeoForgeClasses {
    private val CLASSES = listOf(
        // NeoForge Data Pack Extensions
        "net/neoforged/neoforge/common/data/DataMapProvider",
        "net/neoforged/neoforge/common/data/DataMapProvider\$AdvancedBuilder",
        "net/neoforged/neoforge/common/data/DataMapProvider\$Builder",
        "net/neoforged/neoforge/network/configuration/RegistryDataMapNegotiation",
        "net/neoforged/neoforge/network/payload/KnownRegistryDataMapsPayload",
        "net/neoforged/neoforge/network/payload/KnownRegistryDataMapsPayload\$KnownDataMap",
        "net/neoforged/neoforge/network/payload/KnownRegistryDataMapsReplyPayload",
        "net/neoforged/neoforge/network/payload/RegistryDataMapSyncPayload",
        "net/neoforged/neoforge/registries/ClientRegistryManager",
        "net/neoforged/neoforge/registries/ClientRegistryManager\$1MandatoryEntry",
        "net/neoforged/neoforge/registries/DataMapLoader",
        "net/neoforged/neoforge/registries/DataMapLoader\$1WithSource",
        "net/neoforged/neoforge/registries/DataMapLoader\$LoadResult",
        "net/neoforged/neoforge/registries/IRegistryExtension",
        "net/neoforged/neoforge/registries/RegistryManager",
        "net/neoforged/neoforge/registries/datamaps/AdvancedDataMapType",
        "net/neoforged/neoforge/registries/datamaps/AdvancedDataMapType\$Builder",
        "net/neoforged/neoforge/registries/datamaps/DataMapEntry",
        "net/neoforged/neoforge/registries/datamaps/DataMapEntry\$Removal",
        "net/neoforged/neoforge/registries/datamaps/DataMapFile",
        "net/neoforged/neoforge/registries/datamaps/DataMapType",
        "net/neoforged/neoforge/registries/datamaps/DataMapType\$Builder",
        "net/neoforged/neoforge/registries/datamaps/DataMapValueMerger",
        "net/neoforged/neoforge/registries/datamaps/DataMapValueRemover\$Default",
        "net/neoforged/neoforge/registries/datamaps/IWithData",

        // CreativeCore: already loaded while Nytheria Compat scans the NeoForge mods, so it can only be warned about
        "net/neoforged/fml/common/Mod",
    )

    fun replaceForeignCopies() {
        val nytheria_compat = FabricLoader.getInstance().getModContainer(NytheriaCompat.MOD_ID).orElse(null) ?: return
        val launcher = FabricLauncherBase.getLauncher()
        val classLoader = launcher.targetClassLoader
        val replaced = sortedSetOf<String>()

        for (className in CLASSES) {
            val fileName = "$className.class"
            val nytheriaCompatPath = nytheria_compat.findPath(fileName).orElse(null) ?: continue

            // Only when the class the lookup would load differs from Nytheria Compat's (identical copies are fine).
            val url = classLoader.getResource(fileName) ?: continue
            val found = runCatching { url.openStream().use { it.readBytes() } }.getOrNull() ?: continue
            val own = nytheriaCompatPath.readBytes()
            if (found.contentEquals(own))
                continue

            // Classes that are already loaded (fml.common.Mod, used while Nytheria Compat scans the NeoForge mods)
            // can't be a mixin target any more.
            if (launcher.isClassLoaded(className.replace('/', '.'))) {
                if (members(found) != members(own))
                    NytheriaCompat.logger.warn("Nytheria Compat: a Fabric mod bundles a different copy of ${className.replace('/', '.')}, which was already loaded and can't be replaced with Nytheria Compat's")
                continue
            }

            replaced.add(className)
            ClassTinkerers.addReplacement(className) { node -> replaceWith(node, nytheriaCompatPath) }
        }

        if (replaced.isNotEmpty())
            NytheriaCompat.logger.info("Nytheria Compat: Fabric mods bundle ${replaced.size} NeoForge classes that Nytheria Compat provides itself, Nytheria Compat's own versions are used: ${replaced.joinToString(", ") { it.substringAfterLast('/') }}")
    }

    private fun members(bytes: ByteArray): Set<String> {
        val node = ClassNode(Opcodes.ASM9)
        ClassReader(bytes).accept(node, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        return (node.fields.map { "${it.name}:${it.desc}" } + node.methods.map { "${it.name}${it.desc}" }).toSet() + (node.superName ?: "") + node.interfaces
    }

    private fun replaceWith(node: ClassNode, nytheriaCompatPath: Path) {
        if (!nytheriaCompatPath.exists())
            return

        val own = ClassNode(Opcodes.ASM9)
        ClassReader(nytheriaCompatPath.readBytes()).accept(own, 0)

        node.version = own.version
        node.access = own.access
        node.name = own.name
        node.signature = own.signature
        node.superName = own.superName
        node.interfaces = own.interfaces
        node.sourceFile = own.sourceFile
        node.sourceDebug = own.sourceDebug
        node.module = own.module
        node.outerClass = own.outerClass
        node.outerMethod = own.outerMethod
        node.outerMethodDesc = own.outerMethodDesc
        node.visibleAnnotations = own.visibleAnnotations
        node.invisibleAnnotations = own.invisibleAnnotations
        node.visibleTypeAnnotations = own.visibleTypeAnnotations
        node.invisibleTypeAnnotations = own.invisibleTypeAnnotations
        node.attrs = own.attrs
        node.innerClasses = own.innerClasses
        node.nestHostClass = own.nestHostClass
        node.nestMembers = own.nestMembers
        node.permittedSubclasses = own.permittedSubclasses
        node.recordComponents = own.recordComponents
        node.fields = own.fields
        node.methods = own.methods
    }
}
