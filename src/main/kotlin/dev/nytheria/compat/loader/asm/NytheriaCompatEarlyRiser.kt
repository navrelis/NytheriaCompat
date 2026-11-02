/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.asm

import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import xyz.bluspring.fork.mm.api.ClassTinkerers
import dev.nytheria.compat.NytheriaCompat
import dev.nytheria.compat.loader.remap.fixers.AnnotationWorkaroundFixer
import dev.nytheria.compat.loader.remap.fixers.EnvironmentRemapper
import dev.nytheria.compat.loader.remap.fixers.EventClassVisibilityFixer
import dev.nytheria.compat.util.NytheriaCompatHelper
import dev.nytheria.compat.util.NytheriaCompatTimings
import java.lang.reflect.Modifier
import java.net.URL

class NytheriaCompatEarlyRiser : Runnable {
    companion object {
        /**
         * Nytheria Compat: makes an instance method start with a call of the given no-argument method on `this` whose result is
         * not used. The result has to be popped again: left on the stack it sat below everything the method did, one
         * slot more than the method's maximum stack size allows, and Mixin's analysis of the method for a
         * local-capturing injection of another mod failed with "AnalyzerException: Insufficient maximum stack size"
         * on every start. With the pop the stack is empty again where the original code starts, so the stack map
         * frames of the method stay valid as they are.
         */
        @JvmStatic
        fun insertDiscardedCall(methodNode: MethodNode, owner: String, name: String, descriptor: String) {
            methodNode.instructions.insert(InsnList().apply {
                this.add(VarInsnNode(Opcodes.ALOAD, 0))
                this.add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, descriptor))
                this.add(InsnNode(if (Type.getReturnType(descriptor).size == 2) Opcodes.POP2 else Opcodes.POP))
            })

            // The call itself needs as many slots as its result (at least the one for `this`).
            methodNode.maxStack = maxOf(methodNode.maxStack, Type.getReturnType(descriptor).size, 1)
        }
    }

    override fun run() {
        NytheriaCompatTimings.time("early riser: processForgeClasses") { processForgeClasses() }
        NeoForgeDataPackExtensionsCompat.apply()

        val mappingResolver = FabricLoader.getInstance().mappingResolver
        val namespace = "intermediary"

        // i haven't created a way to replace the abstracted methods yet, so this will do.
        // Nytheria Compat: BakedModel is a client class, on a server there is nothing to transform and Mixin would warn about the target.
        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            val bakedModelIm = "net.minecraft.class_1087"
            val itemTransformsIm = "net.minecraft.class_809"
            val bakedModel = mappingResolver.mapClassName(namespace, bakedModelIm)

            ClassTinkerers.addTransformation(bakedModel) { classNode ->
                run {
                    val getTransformsName = mappingResolver.mapMethodName(namespace, bakedModelIm, "method_4709", "()L${itemTransformsIm.replace(".", "/")};")

                    classNode.methods.removeIf { it.name == getTransformsName }

                    val itemTransforms = mappingResolver.mapClassName(namespace, itemTransformsIm).replace(".", "/")
                    val noTransforms = mappingResolver.mapFieldName(namespace, itemTransformsIm, "field_4301", "L${itemTransformsIm.replace(".", "/")};")

                    // this method should look like this
                    /*
                    default ItemTransforms getTransforms() {
                        return ItemTransforms.NO_TRANSFORMS;
                    }
                     */

                    val getTransform = classNode.visitMethod(Opcodes.ACC_PUBLIC, getTransformsName, "()L$itemTransforms;", null, null)
                    getTransform.visitCode()

                    val label0 = Label()
                    val label1 = Label()

                    getTransform.visitLabel(label0)
                    getTransform.visitFieldInsn(Opcodes.GETSTATIC, itemTransforms, noTransforms, "L$itemTransforms;")
                    getTransform.visitInsn(Opcodes.ARETURN)

                    getTransform.visitLabel(label1)
                    getTransform.visitLocalVariable("this", "L${bakedModel.replace(".", "/")};", null, label0, label1, 0)
                    getTransform.visitMaxs(1, 1)
                    getTransform.visitEnd()
                }
            }
        }

        // BucketItem and LiquidBlock require special treatment as there is currently a weird issue
        // where @Inject doesn't actually properly allow for injecting into multiple targets.
        // TODO: Remove this when that bug is fixed
        run {
            run {
                val flowingFluidIm = "net.minecraft.class_3609"
                val liquidBlockIm = "net.minecraft.class_2404"
                val flowingFluid = mappingResolver.mapClassName(namespace, flowingFluidIm).replace(".", "/")
                val liquidBlock = mappingResolver.mapClassName(namespace, liquidBlockIm).replace(".", "/")

                ClassTinkerers.addTransformation(liquidBlock) {
                    it.methods.forEach { methodNode ->
                        if (methodNode.name.startsWith("<") || Modifier.isStatic(methodNode.access) || Modifier.isAbstract(methodNode.access))
                            return@forEach

                        if (methodNode.instructions.none { a -> a is FieldInsnNode && a.name == mappingResolver.mapFieldName(namespace, liquidBlockIm, "field_11279", "L${flowingFluidIm.replace(".", "/")};") })
                            return@forEach

                        insertDiscardedCall(methodNode, liquidBlock, "getFluid", "()L$flowingFluid;")
                    }
                }
            }
        }

        // Forcefully load the Forge config classes to override Forge Config API Port.
        run {
            val modifiedConfigClasses = listOf(
                "net.neoforged.fml.config.ConfigTracker",
                "net.neoforged.fml.config.ConfigWatcher",
                "net.neoforged.fml.config.IConfigSpec",
                "net.neoforged.fml.config.LoadedConfig",
                "net.neoforged.fml.config.ModConfig",
                "net.neoforged.fml.config.ModConfigs",
                "net.neoforged.neoforge.client.gui.ConfigurationScreen",
                "net.neoforged.neoforge.common.ModConfigSpec",
                $$"net.neoforged.neoforge.common.ModConfigSpec$BooleanValue",
                $$"net.neoforged.neoforge.common.ModConfigSpec$Builder",
                $$"net.neoforged.neoforge.common.ModConfigSpec$BuilderContext",
                $$"net.neoforged.neoforge.common.ModConfigSpec$ConfigValue",
                $$"net.neoforged.neoforge.common.ModConfigSpec$DoubleValue",
                $$"net.neoforged.neoforge.common.ModConfigSpec$EnumValue",
                $$"net.neoforged.neoforge.common.ModConfigSpec$IntValue",
                $$"net.neoforged.neoforge.common.ModConfigSpec$ListValueSpec",
                $$"net.neoforged.neoforge.common.ModConfigSpec$LongValue",
                $$"net.neoforged.neoforge.common.ModConfigSpec$RestartType",
                $$"net.neoforged.neoforge.common.ModConfigSpec$Range",
                $$"net.neoforged.neoforge.common.ModConfigSpec$ValueSpec",
                "net.neoforged.neoforge.common.TranslatableEnum",
            )

            val helperUrl = NytheriaCompat::class.java.classLoader.getResource("net/neoforged/neoforge/common/NeoForge.class")!!

            for (className in modifiedConfigClasses) {
                ClassTinkerers.addReplacement(className) { classNode ->
                    // Reset everything to a blank state, because fuck that
                    classNode.fields?.clear()
                    classNode.methods?.clear()
                    classNode.visibleAnnotations?.clear()
                    classNode.invisibleAnnotations?.clear()
                    classNode.visibleTypeAnnotations?.clear()
                    classNode.invisibleTypeAnnotations?.clear()

                    val url = URL(
                        helperUrl.protocol, helperUrl.host, helperUrl.port, helperUrl.file
                            .replace("net/neoforged/neoforge/common/NeoForge", className.replace(".", "/"))
                    )
                    val classReader = ClassReader(url.readBytes())
                    classReader.accept(classNode, 0)
                }
            }

            // Nytheria Compat: the same for any other NeoForge classes that Fabric mods bundle, see ForeignNeoForgeClasses.
            NytheriaCompatTimings.time("early riser: foreign NeoForge classes") {
                ForeignNeoForgeClasses.replaceForeignCopies()
            }
        }
    }

    private val ignoredKeywords = listOf("nytheria_compat", "fml", "neoforgespi", "mixin", "modlauncher", "kotlinforforge", "architectury", "versions", "internal")

    // Required as Forge runs itself through ASM to fix events and ObjectHolders and such using ModLauncher.
    // So annoying.
    // Nytheria Compat: only for the NeoForge classes that the fixers would change. A public class without these annotations stays as it is.
    private fun processForgeClasses() {
        NytheriaCompatHelper.forEachForgeClass("net/neoforged/") { classNode ->
            if (ignoredKeywords.any { classNode.name.lowercase().contains(it) } || !needsFixers(classNode))
                return@forEachForgeClass

            ClassTinkerers.addTransformation(classNode.name) {
                EventClassVisibilityFixer.fixClass(it)
                AnnotationWorkaroundFixer.fixClass(it)
                EnvironmentRemapper.remapClass(it)
            }
        }
    }

    private fun needsFixers(classNode: ClassNode): Boolean {
        if ((classNode.access and Opcodes.ACC_PUBLIC) == 0)
            return true

        val annotations = sequenceOf(classNode.visibleAnnotations, classNode.invisibleAnnotations) +
            classNode.fields.asSequence().flatMap { sequenceOf(it.visibleAnnotations, it.invisibleAnnotations) } +
            classNode.methods.asSequence().flatMap { sequenceOf(it.visibleAnnotations, it.invisibleAnnotations) }

        return annotations.filterNotNull().flatten().any { annotation ->
            annotation.desc.contains("SubscribeEvent") || annotation.desc == EnvironmentRemapper.ONLYIN_TYPE.descriptor ||
                annotation.desc == EnvironmentRemapper.ONLYINS_TYPE.descriptor || annotation.desc == AnnotationWorkaroundFixer.GAME_TEST_WORKAROUND_DESCRIPTOR
        }
    }
}
