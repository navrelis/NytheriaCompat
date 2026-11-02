/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.mixin.modifications

import com.bawnorton.mixinsquared.reflection.MixinInfoExtension
import com.bawnorton.mixinsquared.reflection.StateExtension
import com.bawnorton.mixinsquared.reflection.TargetClassContextExtension
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.FabricUtil
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.transformer.ext.IExtension
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext
import dev.nytheria.compat.NytheriaCompat
import dev.nytheria.compat.loader.mixin.modifications.modifiers.AnnotationBasedModifier
import dev.nytheria.compat.loader.mixin.modifications.modifiers.InjectedShareAccessModifier

class NytheriaCompatMixinModifier : IExtension {
    override fun checkActive(environment: MixinEnvironment): Boolean {
        return true
    }

    override fun preApply(context: ITargetClassContext) {
        // Ignore classes that aren't modified
        if (!NytheriaCompatMixinModifications.MIXIN_CLASSES.contains(context.classInfo.name))
            return

        TargetClassContextExtension.tryAs(context) { ext ->
            for (mixinInfo in ext.mixins) {
                val modId = mixinInfo.config.getDecoration<String>(FabricUtil.KEY_MOD_ID)

                // Essential mod causes a null modId to appear
                if (modId == null || !NytheriaCompat.loader.hasMod(modId))
                    // Ignore non-Forge mods
                    continue

                val mixinClassNode = mixinInfo.getClassNode(0)
                var wasModified = false

                for (methodNode in mixinClassNode.methods) {
                    val annotations = methodNode.visibleAnnotations ?: continue
                    val newAnnotations = mutableListOf<AnnotationNode>()

                    modifierApplier@for (annotation in annotations) {
                        val modifiers = NytheriaCompatMixinModifications.findMatchingModifiers(context.classInfo.name, annotation, methodNode.desc)

                        if (modifiers.isEmpty()) {
                            newAnnotations.add(annotation)
                            continue
                        }

                        for (modifier in modifiers) {
                            when (modifier) {
                                is AnnotationBasedModifier -> {
                                    modifier.modifyMixin(context.classInfo, annotation, newAnnotations)
                                    wasModified = true
                                }

                                is InjectedShareAccessModifier -> {
                                    // This modifier does nothing here, it's in the fixers instead.
                                    //newAnnotations.add(annotation)
                                    continue
                                }
                            }
                        }
                    }

                    if (wasModified) {
                        methodNode.visibleAnnotations = mutableListOf()
                        methodNode.visibleAnnotations.clear()
                        methodNode.visibleAnnotations.addAll(newAnnotations)
                    }
                }

                if (wasModified) {
                    MixinInfoExtension.tryAs(mixinInfo) {
                        StateExtension.tryAs(it.state) { state ->
                            state.setClassNode(mixinClassNode)
                        }
                    }
                }
            }
        }
    }

    override fun postApply(context: ITargetClassContext) {
    }

    override fun export(env: MixinEnvironment, name: String, force: Boolean, classNode: ClassNode) {
    }
}
