/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap.fixers

import net.fabricmc.loader.api.FabricLoader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import dev.nytheria.compat.loader.remap.NytheriaCompatRemapper

// Porting Lib makes some methods virtual instead of static.
// Let's remap the methods that conflict with Porting Lib.
object ConflictingStaticMethodFixer {
    private val mappingResolver = FabricLoader.getInstance().mappingResolver

    private val conflictingMethods = mapOf(
        mappingResolver.mapClassName("intermediary", "net.minecraft.class_3499").replace(".", "/") to listOf(
            "transformedVec3d" to NytheriaCompatRemapper.remapDescriptor("(Lnet/minecraft/class_3492;Lnet/minecraft/class_243;)Lnet/minecraft/class_243;"),
            "processEntityInfos" to NytheriaCompatRemapper.remapDescriptor("(Lnet/minecraft/class_3499;Lnet/minecraft/class_1936;Lnet/minecraft/class_2338;Lnet/minecraft/class_3492;Ljava/util/List;)Ljava/util/List;")
        )
    )

    fun fixClass(classNode: ClassNode) {
        for (method in classNode.methods) {
            val newNodeMap = mutableMapOf<AbstractInsnNode, AbstractInsnNode>()

            for (insnNode in method.instructions) {
                // Target static invokes specifically
                if (insnNode is MethodInsnNode && insnNode.opcode == Opcodes.INVOKESTATIC) {
                    val specificClass = conflictingMethods.keys.firstOrNull { it == insnNode.owner } ?: continue
                    val methodList = conflictingMethods[specificClass]!!

                    if (methodList.none { it.first == insnNode.name && it.second == insnNode.desc })
                        continue

                    // prefix with Forge
                    val node = MethodInsnNode(insnNode.opcode, insnNode.owner, "forge\$${insnNode.name}", insnNode.desc)
                    newNodeMap[insnNode] = node
                }
            }

            if (newNodeMap.isNotEmpty()) {
                for ((oldNode, newNode) in newNodeMap) {
                    method.instructions.set(oldNode, newNode)
                }
            }
        }
    }
}
