/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap.fixers.mixin

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import dev.nytheria.compat.loader.mixin.modifications.NytheriaCompatMixinModifications
import dev.nytheria.compat.loader.mixin.modifications.modifiers.InjectedShareAccessModifier
import dev.nytheria.compat.util.NytheriaCompatHelper

object MixinDirectModifierFixer {
    fun fixClass(classNode: ClassNode) {
        val targetClassNames = MixinRemapper.getMixinClassTargets(classNode)
        val removedMethods = mutableListOf<MethodNode>()
        val addedMethods = mutableListOf<MethodNode>()

        for (targetClass in targetClassNames) {
            for (methodNode in classNode.methods) {
                val annotations = NytheriaCompatHelper.mergeNullableCollections(methodNode.visibleAnnotations, methodNode.invisibleAnnotations)
                for (annotation in annotations) {
                    val modifiers = NytheriaCompatMixinModifications.findMatchingModifiers(targetClass, annotation, methodNode.desc)
                    val shareAccessModifiers = modifiers.filterIsInstance<InjectedShareAccessModifier>()

                    for (modifier in shareAccessModifiers) {
                        // We want to conflict as little as possible, so we're doing this.
                        val copiedMethodName = $$"nytheriacompat$modified_share_access$$${targetClass.split("/").last()}$$${methodNode.name}$$${annotation.desc.split("/").last().removeSuffix(";")}"

                        // Copy the original method entirely.
                        run {
                            val copiedMethod = MethodNode()
                            copiedMethod.name = copiedMethodName
                            copiedMethod.desc = methodNode.desc
                            copiedMethod.access = methodNode.access

                            copiedMethod.signature = methodNode.signature
                            copiedMethod.visibleAnnotations = methodNode.visibleAnnotations?.toMutableList()?.apply {
                                remove(annotation)
                            }
                            copiedMethod.invisibleAnnotations = methodNode.invisibleAnnotations?.toMutableList()?.apply {
                                remove(annotation)
                            }
                            copiedMethod.visibleTypeAnnotations = methodNode.visibleTypeAnnotations
                            copiedMethod.invisibleTypeAnnotations = methodNode.invisibleTypeAnnotations
                            copiedMethod.invisibleParameterAnnotations = methodNode.invisibleParameterAnnotations
                            copiedMethod.invisibleAnnotableParameterCount = methodNode.invisibleAnnotableParameterCount
                            copiedMethod.visibleAnnotableParameterCount = methodNode.visibleAnnotableParameterCount
                            copiedMethod.visibleLocalVariableAnnotations = methodNode.visibleLocalVariableAnnotations
                            copiedMethod.invisibleLocalVariableAnnotations = methodNode.invisibleLocalVariableAnnotations
                            copiedMethod.tryCatchBlocks = methodNode.tryCatchBlocks
                            copiedMethod.attrs = methodNode.attrs
                            copiedMethod.exceptions = methodNode.exceptions
                            copiedMethod.localVariables = methodNode.localVariables
                            copiedMethod.maxStack = methodNode.maxStack
                            copiedMethod.maxLocals = methodNode.maxLocals

                            copiedMethod.instructions = InsnList()
                            copiedMethod.instructions.insert(methodNode.instructions)

                            addedMethods.add(copiedMethod)
                        }

                        // Throw the handling over to our mixin modifier.
                        val newMethod = modifier.injectShareAccess(classNode, methodNode, annotation, copiedMethodName)

                        removedMethods.add(methodNode)
                        addedMethods.add(newMethod)
                    }
                }
            }
        }

        classNode.methods.removeAll(removedMethods)
        classNode.methods.addAll(addedMethods)
    }
}
