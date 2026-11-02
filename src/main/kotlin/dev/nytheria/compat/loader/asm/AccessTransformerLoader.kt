/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.asm

import net.fabricmc.loader.impl.FabricLoaderImpl
import net.fabricmc.loader.impl.lib.classtweaker.api.ClassTweaker
import net.fabricmc.loader.impl.lib.classtweaker.api.visitor.AccessWidenerVisitor
import org.objectweb.asm.Opcodes
import org.slf4j.LoggerFactory
import xyz.bluspring.fork.mm.api.ClassTinkerers
import dev.nytheria.compat.loader.NytheriaCompatFlags
import dev.nytheria.compat.loader.NytheriaCompatLoader
import dev.nytheria.compat.loader.NytheriaCompatScanCache
import dev.nytheria.compat.loader.remap.NytheriaCompatRemapper
import java.security.MessageDigest
import java.util.HexFormat
import java.util.regex.Pattern
import kotlin.io.path.*

// A reimplementation of NeoForge's Access Transformers.
// The specification can be found here: https://github.com/neoforged/AccessTransformers/blob/main/FMLAT.md
object AccessTransformerLoader {
    private const val TRANSFORM_PREFIX = "t\t"

    private val logger = LoggerFactory.getLogger("Nytheria Compat Access Transformers")
    private val debug = NytheriaCompatFlags.ENABLE_ACCESS_TRANSFORMER_DEBUG
    private var hasLoaded = false

    private val classTransformInfo = mutableMapOf<String, ClassTransformInfo>()

    private val whitespace = Pattern.compile("[ \t]+")

    private fun println(info: String) {
        if (debug)
            logger.info(info)
        else
            logger.debug(info)
    }

    /**
     * Nytheria Compat: the widening of vanilla classes needs the member mappings and the class hierarchy. Its result only depends on the
     * transformers and the translator, so it is written to the Nytheria Compat cache and replayed from there, without any mappings.
     */
    fun convertTransformers(transformers: List<ByteArray>) {
        val widener = Widener(FabricLoaderImpl.INSTANCE.classTweaker)
        val cacheFile = NytheriaCompatScanCache.translatorIdentity?.let { identity ->
            val digest = MessageDigest.getInstance("MD5").apply {
                update(identity.toByteArray())
                transformers.forEach(::update)
            }

            NytheriaCompatLoader.nytheriaCompatCacheDir / "accessTransformers_${HexFormat.of().formatHex(digest.digest())}.txt"
        }

        if (cacheFile != null && cacheFile.exists()) {
            cacheFile.forEachLine { line ->
                if (line.startsWith(TRANSFORM_PREFIX))
                    handleLine(line.removePrefix(TRANSFORM_PREFIX), widener, widen = false)
                else
                    widener.replay(line)
            }

            return
        }

        for (data in transformers) {
            val textData = String(data)
            val delimiter = if (textData.contains("\r\n")) "\r\n" else "\n"

            for ((index, line) in textData.split(delimiter).withIndex()) {
                if (line.trim().startsWith("#"))
                    continue

                if (line.isBlank())
                    continue

                try {
                    handleLine(line, widener, widen = true)
                } catch (e: Throwable) {
                    throw RuntimeException("Failed to process access transformer at line ${index + 1}: \"$line\"", e)
                }
            }
        }

        if (cacheFile != null) {
            NytheriaCompatLoader.nytheriaCompatCacheDir.listDirectoryEntries("accessTransformers_*.txt").forEach { it.deleteIfExists() }
            cacheFile.writeLines(widener.log)
        }
    }

    private fun handleLine(line: String, widener: Widener, widen: Boolean) {
        val split = line.trim().split(whitespace)

        // class name
        val mojClassName = split[1].replace(".", "/")

        val isVanillaClass = mojClassName.startsWith("net/minecraft/") || mojClassName.startsWith("com/mojang/")
        val handledViaAccessWidener =
            widen && isVanillaClass && widenAccessForVanillaClasses(split, mojClassName, widener) // Handle access widening over Fabric

        if (!handledViaAccessWidener) {
            // Nytheria Compat: a Mojang-named class of the game is called something else when it loads, so a transformer for it never matches
            //  (the line names a member the mappings don't have, mods still ship stale SRG names) and only makes Mixin warn about its target.
            if (mojClassName.startsWith("net/minecraft/"))
                return

            // Otherwise, this is for every other class
            if (widen)
                widener.log.add(TRANSFORM_PREFIX + line)

            handleAccessTransform(split, mojClassName)
        }
    }

    /**
     * Applies access widening to the game and logs each step as one tab-separated line, for [replay].
     */
    private class Widener(private val classTweaker: ClassTweaker) {
        val log = mutableListOf<String>()

        fun visitClass(owner: String, type: AccessWidenerVisitor.AccessType) = apply("c", owner, "", "", type)

        fun visitField(owner: String, name: String, descriptor: String, type: AccessWidenerVisitor.AccessType) = apply("f", owner, name, descriptor, type)

        fun visitMethod(owner: String, name: String, descriptor: String, type: AccessWidenerVisitor.AccessType) = apply("m", owner, name, descriptor, type)

        private fun apply(kind: String, owner: String, name: String, descriptor: String, type: AccessWidenerVisitor.AccessType) {
            log.add("$kind\t$owner\t$name\t$descriptor\t${type.name}")
            run(kind, owner, name, descriptor, type)
        }

        fun replay(line: String) {
            val (kind, owner, name, descriptor, type) = line.split('\t')
            run(kind, owner, name, descriptor, AccessWidenerVisitor.AccessType.valueOf(type))
        }

        private fun run(kind: String, owner: String, name: String, descriptor: String, type: AccessWidenerVisitor.AccessType) {
            val visitor = classTweaker.visitAccessWidener(owner)!! // it shouldn't be possible for this to be null.

            when (kind) {
                "c" -> visitor.visitClass(type, true)
                "f" -> visitor.visitField(name, descriptor, type, true)
                else -> visitor.visitMethod(name, descriptor, type, true)
            }
        }
    }

    private fun handleAccessTransform(split: List<String>, className: String) {
        // access modifier
        val accessType = when (split[0].removeSuffix("-f").removeSuffix("+f")) {
            "public" -> AccessType.PUBLIC
            "protected" -> AccessType.PROTECTED
            "private" -> AccessType.PRIVATE
            "default" -> AccessType.PACKAGE_PRIVATE
            else -> throw IllegalArgumentException("Expected public/protected/private/default, got ${split[0]}")
        }

        val finalType = if (split[0].endsWith("-f"))
            Final.REMOVE
        else if (split[0].endsWith("+f"))
            Final.ADD
        else Final.DEFAULT

        // field / method
        if (split.size > 2 && !split[2].startsWith("#")) {
            if (split[2].contains("(")) { // method
                var name = ""
                var descriptor = ""

                run { // get descriptor
                    var isInDescriptor = false

                    for (char in split[2]) {
                        if (char == '(')
                            isInDescriptor = true

                        if (isInDescriptor)
                            descriptor += char
                        else
                            name += char
                    }
                }

                val methodName = name
                val transformInfo = classTransformInfo[className] ?: ClassTransformInfo(AccessType.DEFAULT, Final.DEFAULT)
                val pair = Pair(methodName, descriptor)

                val priorityAccess = accessType
                val priorityFinal = finalType

                if (transformInfo.methods.contains(pair)) {
                    val methodTransformInfo = transformInfo.methods[pair]!!

                    // promote access type
                    if (priorityAccess.ordinal < methodTransformInfo.currentAccessType.ordinal) {
                        methodTransformInfo.currentAccessType = priorityAccess
                    }

                    // promote final type
                    if (priorityFinal.ordinal < methodTransformInfo.final.ordinal) {
                        methodTransformInfo.final = priorityFinal
                    }
                } else {
                    transformInfo.methods[pair] = TransformInfo(priorityAccess, priorityFinal)
                }

                if (!classTransformInfo.contains(className))
                    classTransformInfo[className] = transformInfo
            } else { // field
                val name = split[2]

                val fieldName = name

                val transformInfo = classTransformInfo[className] ?: ClassTransformInfo(AccessType.DEFAULT, Final.DEFAULT)

                if (transformInfo.fields.contains(fieldName)) {
                    val fieldTransformInfo = transformInfo.fields[fieldName]!!

                    // promote access type
                    if (accessType.ordinal < fieldTransformInfo.currentAccessType.ordinal) {
                        fieldTransformInfo.currentAccessType = accessType
                    }

                    // promote final type
                    if (finalType.ordinal < fieldTransformInfo.final.ordinal) {
                        fieldTransformInfo.final = finalType
                    }
                } else {
                    transformInfo.fields[fieldName] = TransformInfo(accessType, finalType)
                }

                if (!classTransformInfo.contains(className))
                    classTransformInfo[className] = transformInfo
            }
        } else { // class
            val transformInfo = classTransformInfo[className]

            val priorityAccess = accessType

            val priorityFinal = finalType

            if (transformInfo == null) {
                classTransformInfo[className] = ClassTransformInfo(priorityAccess, priorityFinal)
            } else {
                // Promote the access type if the level is higher than the current one
                if (priorityAccess.ordinal < transformInfo.currentAccessType.ordinal) {
                    transformInfo.currentAccessType = priorityAccess
                }

                // Also promote the final type
                if (priorityFinal.ordinal < transformInfo.final.ordinal) {
                    transformInfo.final = priorityFinal
                }
            }
        }
    }

    fun runTransformers() {
        if (hasLoaded)
            return

        val startTime = System.currentTimeMillis()
        logger.info("Adding access transformers to mixin")

        classTransformInfo.forEach { (mappedClassName, classTransformInfo) ->
            ClassTinkerers.addTransformation(mappedClassName) { classNode ->
                println("access transforming class $mappedClassName")

                // access modifiers
                if (classTransformInfo.currentAccessType != AccessType.DEFAULT) {
                    AccessType.values().forEach { accessType ->
                        // clear bits
                        classNode.access = classNode.access and accessType.flag.inv()
                    }

                    classNode.access = classNode.access or classTransformInfo.currentAccessType.flag
                    println("set class to access type ${classTransformInfo.currentAccessType.name}")
                }

                // final flag
                if (classTransformInfo.final != Final.DEFAULT) {
                    classNode.access = if (classTransformInfo.final == Final.ADD)
                        classNode.access or Opcodes.ACC_FINAL // set bits
                    else
                        classNode.access and Opcodes.ACC_FINAL.inv() // clear bits

                    println("set class to final type ${classTransformInfo.final.name}")
                }

                classTransformInfo.fields.forEach field@{ (fieldName, fieldTransformInfo) ->
                    val mappedFieldName = fieldName

                    println("transforming field $mappedFieldName")

                    classNode.fields.filter { it.name == mappedFieldName || mappedFieldName == "*" }.forEach { fieldNode ->
                        // access modifiers
                        if (fieldTransformInfo.currentAccessType != AccessType.DEFAULT) {
                            AccessType.values().forEach { accessType ->
                                // clear bits
                                fieldNode.access = fieldNode.access and accessType.flag.inv()
                            }

                            // add bits
                            fieldNode.access = fieldNode.access or fieldTransformInfo.currentAccessType.flag

                            println("set field to access type ${fieldTransformInfo.currentAccessType.name}")
                        }

                        // final flag
                        if (fieldTransformInfo.final != Final.DEFAULT) {
                            if (fieldTransformInfo.final == Final.ADD)
                            // add bits
                                fieldNode.access = fieldNode.access or Opcodes.ACC_FINAL
                            else
                            // clear bits
                                fieldNode.access = fieldNode.access and Opcodes.ACC_FINAL.inv()

                            println("set field to final type ${fieldTransformInfo.final.name}")
                        }
                    }

                    if (mappedFieldName == "*") {
                        classNode.methods.forEach { methodNode ->
                            // access modifiers
                            if (fieldTransformInfo.currentAccessType != AccessType.DEFAULT) {
                                AccessType.values().forEach { accessType ->
                                    // clear bits
                                    methodNode.access = methodNode.access and accessType.flag.inv()
                                }

                                // add bits
                                methodNode.access = methodNode.access or fieldTransformInfo.currentAccessType.flag

                                println("set method to access type ${fieldTransformInfo.currentAccessType.name}")
                            }

                            // final flag
                            if (fieldTransformInfo.final != Final.DEFAULT) {
                                if (fieldTransformInfo.final == Final.ADD)
                                // add bits
                                    methodNode.access = methodNode.access or Opcodes.ACC_FINAL
                                else
                                // clear bits
                                    methodNode.access = methodNode.access and Opcodes.ACC_FINAL.inv()

                                println("set method to final type ${fieldTransformInfo.final.name}")
                            }
                        }
                    }
                }

                classTransformInfo.methods.forEach method@{ (pair, methodTransformInfo) ->
                    val mappedMethodName = pair.first
                    val mappedDescriptor = pair.second

                    println("transforming method $mappedMethodName$mappedDescriptor")

                    classNode.methods.filter { (it.name == mappedMethodName || it.name == "*") && (it.desc == "()" || it.desc == mappedDescriptor) }.forEach { methodNode ->
                        // access modifiers
                        if (methodTransformInfo.currentAccessType != AccessType.DEFAULT) {
                            AccessType.values().forEach { accessType ->
                                // clear bits
                                methodNode.access = methodNode.access and accessType.flag.inv()
                            }

                            // add bits
                            methodNode.access = methodNode.access or methodTransformInfo.currentAccessType.flag

                            println("set method to access type ${methodTransformInfo.currentAccessType.name}")
                        }

                        // final flag
                        if (methodTransformInfo.final != Final.DEFAULT) {
                            if (methodTransformInfo.final == Final.ADD)
                            // add bits
                                methodNode.access = methodNode.access or Opcodes.ACC_FINAL
                            else
                            // clear bits
                                methodNode.access = methodNode.access and Opcodes.ACC_FINAL.inv()

                            println("set method to final type ${methodTransformInfo.final.name}")
                        }
                    }
                }
            }
        }

        logger.info("Finished loading access transformers (took ${System.currentTimeMillis() - startTime}ms)")
        hasLoaded = true
    }

    private fun widenAccessForVanillaClasses(split: List<String>, mojClassName: String, accessWidener: Widener): Boolean {
        val intermediaryClassName = NytheriaCompatRemapper.remapClass(mojClassName)
        val remapper = NytheriaCompatRemapper.enhancedRemapper

        // field / method
        if (split.size > 2 && !split[2].startsWith("#")) {
            if (split[2] == "*") { // Handle all of them
                val classInfo = remapper.getClass(mojClassName).orElse(null)

                if (classInfo == null) {
                    logger.warn("Missing class reference (Mojang: $mojClassName, Intermediary: $intermediaryClassName) for access transform, skipping.")
                    return false
                }

                for (fieldOpt in classInfo.fields) {
                    val field = fieldOpt.orElse(null) ?: continue

                    val mappedDesc = remapper.mapDesc(field.descriptor)
                    accessWidener.visitField(intermediaryClassName, field.mapped, mappedDesc, AccessWidenerVisitor.AccessType.ACCESSIBLE)
                    accessWidener.visitField(intermediaryClassName, field.mapped, mappedDesc, AccessWidenerVisitor.AccessType.MUTABLE)
                }
            } else if (split[2] == "*()") {

                val classInfo = remapper.getClass(mojClassName).orElse(null)

                if (classInfo == null) {
                    logger.warn("Missing class reference (Mojang: $mojClassName, Intermediary: $intermediaryClassName) for access transform, skipping.")
                    return false
                }

                for (methodOpt in classInfo.methods) {
                    val method = methodOpt.orElse(null) ?: continue

                    val mappedMethodDesc = remapper.mapMethodDesc(method.descriptor)
                    accessWidener.visitMethod(intermediaryClassName, method.mapped, mappedMethodDesc, AccessWidenerVisitor.AccessType.ACCESSIBLE)
                    accessWidener.visitMethod(intermediaryClassName, method.mapped, mappedMethodDesc, AccessWidenerVisitor.AccessType.EXTENDABLE)
                }
            } else if (split[2].contains("(")) { // method
                var name = ""
                var descriptor = ""

                run { // get descriptor
                    var isInDescriptor = false

                    for (char in split[2]) {
                        if (char == '(')
                            isInDescriptor = true

                        if (isInDescriptor)
                            descriptor += char
                        else
                            name += char
                    }
                }

                val mappedDescriptor = NytheriaCompatRemapper.remapDescriptor(descriptor)

                if (name == "*") {
                    val cls = remapper.getClass(intermediaryClassName).orElse(null) ?: return false

                    for (methodOpt in cls.methods) {
                        methodOpt.ifPresent {
                            if (descriptor != "()" && it.descriptor != mappedDescriptor)
                                return@ifPresent

                            // write it into Fabric, as otherwise, @Overwrite mixins will not map correctly.
                            accessWidener.visitMethod(intermediaryClassName, it.mapped, it.descriptor, AccessWidenerVisitor.AccessType.ACCESSIBLE)
                            // make sure it's made extendable by default too, as Fabric automatically marks methods as final when made accessible.
                            accessWidener.visitMethod(intermediaryClassName, it.mapped, it.descriptor, AccessWidenerVisitor.AccessType.EXTENDABLE)
                        }
                    }

                    return true
                }

                val methodName = remapper.mapMethodName(mojClassName.replace(".", "/"), name, descriptor)

                // write it into Fabric, as otherwise, @Overwrite mixins will not map correctly.
                accessWidener.visitMethod(intermediaryClassName, methodName, mappedDescriptor, AccessWidenerVisitor.AccessType.ACCESSIBLE)
                // make sure it's made extendable by default too, as Fabric automatically marks methods as final when made accessible.
                accessWidener.visitMethod(intermediaryClassName, methodName, mappedDescriptor, AccessWidenerVisitor.AccessType.EXTENDABLE)
            } else { // field
                val name = split[2]

                val fieldInfo = NytheriaCompatRemapper.mojIntermediaryMapping.getClass(mojClassName)?.fields?.firstOrNull { it.original == name } ?: return false
                val fieldName = NytheriaCompatRemapper.enhancedRemapper.mapFieldName(mojClassName.replace(".", "/"), name, fieldInfo.descriptor ?: return false)

                accessWidener.visitField(intermediaryClassName, fieldName, NytheriaCompatRemapper.remapDescriptor(fieldInfo.descriptor!!), AccessWidenerVisitor.AccessType.ACCESSIBLE)
                accessWidener.visitField(intermediaryClassName, fieldName, NytheriaCompatRemapper.remapDescriptor(fieldInfo.descriptor!!), AccessWidenerVisitor.AccessType.MUTABLE)
            }
        } else { // class
            accessWidener.visitClass(intermediaryClassName, AccessWidenerVisitor.AccessType.ACCESSIBLE)
            accessWidener.visitClass(intermediaryClassName, AccessWidenerVisitor.AccessType.EXTENDABLE)
        }

        return true
    }

    private enum class AccessType(val flag: Int) {
        PUBLIC(Opcodes.ACC_PUBLIC), PROTECTED(Opcodes.ACC_PROTECTED), PACKAGE_PRIVATE(0), PRIVATE(Opcodes.ACC_PRIVATE), DEFAULT(0)
    }

    private enum class Final {
        REMOVE, ADD, DEFAULT
    }

    private data class ClassTransformInfo(
        var currentAccessType: AccessType,
        var final: Final,
        val fields: MutableMap<String, TransformInfo> = mutableMapOf(),
        val methods: MutableMap<Pair<String, String>, TransformInfo> = mutableMapOf()
    )

    private data class TransformInfo(
        var currentAccessType: AccessType,
        var final: Final
    )
}
