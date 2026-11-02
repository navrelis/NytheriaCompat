package dev.nytheria.compat.loader

import net.fabricmc.loader.api.FabricLoader
import net.neoforged.fml.loading.modscan.ModAnnotation
import net.neoforged.neoforgespi.language.ModFileScanData
import org.objectweb.asm.Type
import dev.nytheria.compat.NytheriaCompat
import dev.nytheria.compat.loader.remap.NytheriaCompatRemapper
import java.io.*
import java.lang.annotation.ElementType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

/**
 * Nytheria Compat: a persistent cache of the class scan data of a mod jar (the classes and annotations [NytheriaCompatLoader.scanModClasses]
 * collects), so that a cached start doesn't have to read every class of every mod with ASM again.
 *
 * An entry is only used if everything it was built from is unchanged: the scanned jar (path, size and modification
 * time; the name of a remapped jar also carries the MD5 of the original mod jar and the [NytheriaCompatRemapper.REMAPPER_VERSION]),
 * the translator jar (version, size and modification time, which covers the scanner itself) and
 * [NytheriaCompatRemapper.REMAPPER_VERSION]. Anything else, including an unreadable or unknown entry, falls back to a real scan.
 * Entries are written to a temporary file and atomically moved into place, so concurrent readers and writers (another
 * thread, or another game instance sharing the directory) never see a partial entry.
 */
object NytheriaCompatScanCache {
    private const val MAGIC = 0x4C454353 // "LECS"
    private const val FORMAT_VERSION = 1
    private const val END_MARKER = 0x454E4421

    private val cacheDir = NytheriaCompatLoader.nytheriaCompatCacheDir / "scanCache"

    // The development environment runs the translator from class directories, which have no stable identity.
    internal val translatorIdentity: String? by lazy {
        runCatching {
            val translatorPath = NytheriaCompatLoader::class.java.protectionDomain.codeSource.location.toURI().toPath()
            if (!translatorPath.isRegularFile())
                return@runCatching null

            val version = FabricLoader.getInstance().getModContainer(NytheriaCompat.MOD_ID).orElseThrow().metadata.version.friendlyString
            "$version|${translatorPath.fileSize()}|${translatorPath.getLastModifiedTime().toMillis()}|${NytheriaCompatRemapper.REMAPPER_VERSION}"
        }.getOrNull()
    }

    class Entry(val classes: List<ModFileScanData.ClassData>, val annotations: List<ModFileScanData.AnnotationData>)

    private fun cacheFile(jar: Path): Path = cacheDir / "${jar.fileName}.scan"

    private fun jarIdentity(jar: Path): String {
        return "${jar.toAbsolutePath()}|${jar.fileSize()}|${jar.getLastModifiedTime().toMillis()}"
    }

    fun read(jar: Path): Entry? {
        val translator = translatorIdentity ?: return null
        val file = cacheFile(jar)
        if (!file.isRegularFile())
            return null

        return try {
            DataInputStream(BufferedInputStream(file.inputStream())).use { input ->
                if (input.readInt() != MAGIC || input.readInt() != FORMAT_VERSION)
                    return null
                if (input.readUTF() != translator || input.readUTF() != jarIdentity(jar))
                    return null

                val classCount = input.readInt()
                val classes = ArrayList<ModFileScanData.ClassData>(classCount)
                repeat(classCount) {
                    classes.add(readClassData(input))
                }

                val annotationCount = input.readInt()
                val annotations = ArrayList<ModFileScanData.AnnotationData>(annotationCount)
                repeat(annotationCount) {
                    annotations.add(readAnnotationData(input))
                }

                if (input.readInt() != END_MARKER)
                    return null

                Entry(classes, annotations)
            }
        } catch (e: Exception) {
            NytheriaCompat.logger.warn("Nytheria Compat: Ignoring unreadable class scan cache entry $file", e)
            null
        }
    }

    fun write(jar: Path, classes: List<ModFileScanData.ClassData>, annotations: List<ModFileScanData.AnnotationData>) {
        val translator = translatorIdentity ?: return
        val file = cacheFile(jar)

        try {
            val bytes = ByteArrayOutputStream()
            DataOutputStream(bytes).use { output ->
                output.writeInt(MAGIC)
                output.writeInt(FORMAT_VERSION)
                output.writeUTF(translator)
                output.writeUTF(jarIdentity(jar))

                output.writeInt(classes.size)
                for (data in classes)
                    writeClassData(output, data)

                output.writeInt(annotations.size)
                for (data in annotations)
                    writeAnnotationData(output, data)

                output.writeInt(END_MARKER)
            }

            cacheDir.createDirectories()
            val temp = Files.createTempFile(cacheDir, file.fileName.toString(), ".tmp")
            try {
                temp.writeBytes(bytes.toByteArray())
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } finally {
                temp.deleteIfExists()
            }
        } catch (e: UnsupportedValueException) {
            // An annotation value we can't store; this jar is simply scanned on every start.
            NytheriaCompat.logger.debug("Nytheria Compat: Not caching the class scan of {}: {}", jar.fileName, e.message)
        } catch (e: Exception) {
            NytheriaCompat.logger.warn("Nytheria Compat: Failed to write the class scan cache entry for ${jar.fileName}", e)
        }
    }

    /**
     * Removes the entries of jars that aren't loaded any more (for example a remapped jar of an old mod version).
     */
    fun removeUnusedEntries(usedJars: Collection<Path>) {
        if (!cacheDir.isDirectory())
            return

        val used = usedJars.map { cacheFile(it).fileName.toString() }.toSet()
        runCatching {
            cacheDir.listDirectoryEntries().forEach { file ->
                if (file.fileName.toString() !in used && file.extension == "scan")
                    file.deleteIfExists()
            }
        }
    }

    private class UnsupportedValueException(message: String) : Exception(message)

    // Class types are created with Type.getObjectType by ModClassVisitor, so they are stored as internal names.
    private fun writeObjectType(output: DataOutputStream, type: Type?) {
        output.writeBoolean(type != null)
        if (type != null)
            output.writeUTF(type.internalName)
    }

    private fun readObjectType(input: DataInputStream): Type? {
        return if (input.readBoolean()) Type.getObjectType(input.readUTF()) else null
    }

    private fun writeNullableString(output: DataOutputStream, value: String?) {
        output.writeBoolean(value != null)
        if (value != null)
            output.writeUTF(value)
    }

    private fun readNullableString(input: DataInputStream): String? {
        return if (input.readBoolean()) input.readUTF() else null
    }

    private fun writeClassData(output: DataOutputStream, data: ModFileScanData.ClassData) {
        writeObjectType(output, data.clazz)
        writeObjectType(output, data.parent)
        output.writeInt(data.interfaces.size)
        for (itf in data.interfaces)
            writeObjectType(output, itf)
    }

    private fun readClassData(input: DataInputStream): ModFileScanData.ClassData {
        val clazz = readObjectType(input)
        val parent = readObjectType(input)
        val interfaceCount = input.readInt()
        val interfaces = HashSet<Type>(interfaceCount * 2)
        repeat(interfaceCount) {
            interfaces.add(readObjectType(input)!!)
        }

        return ModFileScanData.ClassData(clazz, parent, interfaces)
    }

    private fun writeAnnotationData(output: DataOutputStream, data: ModFileScanData.AnnotationData) {
        // The annotation type comes from Type.getType(descriptor).
        output.writeUTF(data.annotationType.descriptor)
        output.writeUTF(data.targetType.name)
        writeObjectType(output, data.clazz)
        writeNullableString(output, data.memberName)
        writeValue(output, data.annotationData)
    }

    private fun readAnnotationData(input: DataInputStream): ModFileScanData.AnnotationData {
        val annotationType = Type.getType(input.readUTF())
        val targetType = ElementType.valueOf(input.readUTF())
        val clazz = readObjectType(input)
        val memberName = readNullableString(input)
        @Suppress("UNCHECKED_CAST")
        val values = readValue(input) as MutableMap<String, Any?>

        return ModFileScanData.AnnotationData(annotationType, targetType, clazz, memberName, values)
    }

    // Everything ModAnnotationVisitor can store: the constant types of AnnotationVisitor.visit (boxed primitives,
    // strings, Types and primitive arrays), enum values, arrays (lists) and nested annotations (maps).
    private fun writeValue(output: DataOutputStream, value: Any?) {
        when (value) {
            null -> output.writeByte(0)
            is String -> { output.writeByte(1); output.writeUTF(value) }
            is Type -> { output.writeByte(2); output.writeUTF(value.descriptor) }
            is Boolean -> { output.writeByte(3); output.writeBoolean(value) }
            is Byte -> { output.writeByte(4); output.writeByte(value.toInt()) }
            is Char -> { output.writeByte(5); output.writeChar(value.code) }
            is Short -> { output.writeByte(6); output.writeShort(value.toInt()) }
            is Int -> { output.writeByte(7); output.writeInt(value) }
            is Long -> { output.writeByte(8); output.writeLong(value) }
            is Float -> { output.writeByte(9); output.writeFloat(value) }
            is Double -> { output.writeByte(10); output.writeDouble(value) }
            is ModAnnotation.EnumHolder -> { output.writeByte(11); output.writeUTF(value.desc); output.writeUTF(value.value) }
            is List<*> -> {
                output.writeByte(12)
                output.writeInt(value.size)
                for (element in value)
                    writeValue(output, element)
            }
            is Map<*, *> -> {
                output.writeByte(13)
                output.writeInt(value.size)
                for ((key, element) in value) {
                    output.writeUTF(key as? String ?: throw UnsupportedValueException("annotation key $key"))
                    writeValue(output, element)
                }
            }
            is ByteArray -> { output.writeByte(14); output.writeInt(value.size); value.forEach { output.writeByte(it.toInt()) } }
            is BooleanArray -> { output.writeByte(15); output.writeInt(value.size); value.forEach { output.writeBoolean(it) } }
            is CharArray -> { output.writeByte(16); output.writeInt(value.size); value.forEach { output.writeChar(it.code) } }
            is ShortArray -> { output.writeByte(17); output.writeInt(value.size); value.forEach { output.writeShort(it.toInt()) } }
            is IntArray -> { output.writeByte(18); output.writeInt(value.size); value.forEach { output.writeInt(it) } }
            is LongArray -> { output.writeByte(19); output.writeInt(value.size); value.forEach { output.writeLong(it) } }
            is FloatArray -> { output.writeByte(20); output.writeInt(value.size); value.forEach { output.writeFloat(it) } }
            is DoubleArray -> { output.writeByte(21); output.writeInt(value.size); value.forEach { output.writeDouble(it) } }
            else -> throw UnsupportedValueException("annotation value of type ${value.javaClass.name}")
        }
    }

    private fun readValue(input: DataInputStream): Any? {
        return when (val tag = input.readByte().toInt()) {
            0 -> null
            1 -> input.readUTF()
            2 -> Type.getType(input.readUTF())
            3 -> input.readBoolean()
            4 -> input.readByte()
            5 -> input.readChar()
            6 -> input.readShort()
            7 -> input.readInt()
            8 -> input.readLong()
            9 -> input.readFloat()
            10 -> input.readDouble()
            11 -> ModAnnotation.EnumHolder(input.readUTF(), input.readUTF())
            12 -> {
                val size = input.readInt()
                val list = ArrayList<Any?>(size)
                repeat(size) { list.add(readValue(input)) }
                list
            }
            13 -> {
                val size = input.readInt()
                val map = HashMap<String, Any?>(size * 2)
                repeat(size) { map[input.readUTF()] = readValue(input) }
                map
            }
            14 -> ByteArray(input.readInt()) { input.readByte() }
            15 -> BooleanArray(input.readInt()) { input.readBoolean() }
            16 -> CharArray(input.readInt()) { input.readChar() }
            17 -> ShortArray(input.readInt()) { input.readShort() }
            18 -> IntArray(input.readInt()) { input.readInt() }
            19 -> LongArray(input.readInt()) { input.readLong() }
            20 -> FloatArray(input.readInt()) { input.readFloat() }
            21 -> DoubleArray(input.readInt()) { input.readDouble() }
            else -> throw IOException("Unknown value tag $tag")
        }
    }
}
