/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap.fixers

import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureWriter
import org.objectweb.asm.tree.ClassNode
import dev.nytheria.compat.loader.remap.NytheriaCompatRemapper

object ConditionalInterfaceInjectionFixer {
    private val forgeMethodLookup = mapOf(
        ("getLightEmission" to NytheriaCompatRemapper.remapDescriptor("(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I")) to "io/github/fabricators_of_create/porting_lib/blocks/extensions/LightEmissiveBlock"
    )

    fun fixClass(classNode: ClassNode) {
        for (method in classNode.methods) {
            if (forgeMethodLookup.contains(method.name to method.desc)) {
                val className = forgeMethodLookup[method.name to method.desc]!!
                classNode.interfaces?.add(className)
                if (classNode.signature != null) {
                    val reader = SignatureReader(classNode.signature)
                    val writer = SignatureWriter()
                    reader.accept(writer)
                    writer.visitClassType(className)
                    classNode.signature = writer.toString()
                }
            }
        }
    }
}
