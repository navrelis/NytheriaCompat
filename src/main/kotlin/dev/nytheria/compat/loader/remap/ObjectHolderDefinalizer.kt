/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

object ObjectHolderDefinalizer {
    private const val OBJECT_HOLDER = "Lnet/minecraftforge/registries/ObjectHolder;"
    private const val FLAGS = Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL

    private fun hasHolder(list: List<AnnotationNode>?): Boolean {
        return list != null && list.any { it.desc == OBJECT_HOLDER }
    }

    fun processClass(classNode: ClassNode) {
        // We don't do this on Vanilla classes, so it's as simple as this.
        classNode.fields.forEach {
            if ((it.access and FLAGS) == FLAGS && it.desc.startsWith("L") && hasHolder(it.visibleAnnotations)) {
                it.access = (it.access and Opcodes.ACC_FINAL.inv()) or Opcodes.ACC_SYNTHETIC
            }
        }
    }
}