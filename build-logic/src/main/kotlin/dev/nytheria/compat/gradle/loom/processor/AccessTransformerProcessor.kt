/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.gradle.loom.processor

import net.fabricmc.loom.api.processor.MinecraftJarProcessor
import net.fabricmc.loom.api.processor.ProcessorContext
import net.fabricmc.loom.api.processor.SpecContext
import net.fabricmc.loom.util.Pair
import net.fabricmc.loom.util.ZipUtils
import net.fabricmc.loom.util.ZipUtils.UnsafeUnaryOperator
import net.fabricmc.loom.util.fmj.FabricModJson
import net.neoforged.accesstransformer.api.AccessTransformerEngine
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path

open class AccessTransformerProcessor : MinecraftJarProcessor<AccessTransformerProcessor.Spec> {
    override fun buildSpec(context: SpecContext): Spec? {
        return Spec(context.allMods())
    }

    override fun processJar(
        jar: Path,
        spec: Spec,
        context: ProcessorContext
    ) {
        println("Applying Access Transformers")
        spec.mods.forEach { fmj ->
            val at = fmj.getCustom(ACCESS_TRANSFORMER)

            if (at != null) {
                for (entry in at.asJsonArray) {
                    val reader = StringReader(String(fmj.source.read(entry.asString), StandardCharsets.UTF_8))
                    AT_ENGINE.loadAT(reader, entry.asString)
                    println(entry.asString)
                }
            }

            val classes = AT_ENGINE.targets.map { target -> target.className }

            ZipUtils.transform(jar, getTransformers(classes))
        }
    }

    private fun getTransformers(classes: List<String>): MutableList<Pair<String, UnsafeUnaryOperator<ByteArray>>> {
        return classes
            .map { string ->
                Pair(
                    string!!.replace("\\.".toRegex(), "/") + ".class",
                    getTransformer(string)
                )
            }
            .toMutableList()
    }

    private fun getTransformer(className: String): UnsafeUnaryOperator<ByteArray> {
        return UnsafeUnaryOperator { input ->
            val reader = ClassReader(input)
            val writer = ClassWriter(0)
            val cn = ClassNode()
            reader.accept(cn, 0)
            val type = Type.getType("L"+cn.name.replace("\\.","/")+";")
            if (AT_ENGINE.containsClassTarget(type)) {
                println("Applying access transformer to $className")
                AT_ENGINE.transform(cn, type)
            }

            cn.accept(writer)

            writer.toByteArray()
        }
    }

    override fun getName(): String {
        return "nytheria_compat:access-transformer"
    }

    companion object {
        const val ACCESS_TRANSFORMER: String = "nytheria_compat:access_transformer"
        val AT_ENGINE: AccessTransformerEngine = AccessTransformerEngine.newEngine()
        var LOGGER: Logger = LoggerFactory.getLogger(AccessTransformerProcessor::class.java)
    }

    data class Spec(val mods: List<FabricModJson>) : MinecraftJarProcessor.Spec {}
}