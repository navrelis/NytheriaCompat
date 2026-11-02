/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap.fixers

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

object EventClassVisibilityFixer {
    fun fixClass(classNode: ClassNode) {
        for (method in classNode.methods) {
            if (method.visibleAnnotations != null && method.visibleAnnotations.any { it.desc.contains("SubscribeEvent") })
                method.access = (method.access and Opcodes.ACC_PRIVATE.inv() and Opcodes.ACC_PROTECTED.inv()) or Opcodes.ACC_PUBLIC
        }

        // Mark class as public
        classNode.access = (classNode.access and Opcodes.ACC_PRIVATE.inv() and Opcodes.ACC_PROTECTED.inv()) or Opcodes.ACC_PUBLIC
    }
}
