/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap.fixers

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import dev.nytheria.compat.loader.remap.NytheriaCompatRemapper

object InjectedInterfaceVisibilityFixer {
    private val methodsToFix = listOf("getCustomMapData" to NytheriaCompatRemapper.remapDescriptor("(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)"))
    fun fixClass(classNode: ClassNode) {
        for (methodNode in classNode.methods) {
            for (method in methodsToFix) {
                if (methodNode.name == method.first && methodNode.desc.contains(method.second)) {
                    methodNode.access = Opcodes.ACC_PUBLIC
                }
            }
        }
    }
}
