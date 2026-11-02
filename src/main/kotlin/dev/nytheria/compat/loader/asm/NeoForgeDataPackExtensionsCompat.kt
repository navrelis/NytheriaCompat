package dev.nytheria.compat.loader.asm

import net.fabricmc.loader.api.FabricLoader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import xyz.bluspring.fork.mm.api.ClassTinkerers
import dev.nytheria.compat.NytheriaCompat

/**
 * Nytheria Compat: NeoForge Data Pack Extensions (Fuzs, nested in Dyed Flames) is a Fabric port of NeoForge's data maps. It
 * ships its own copies of NeoForge's data map classes under NeoForge's names, but with renamed methods
 * (`neoforgedatapackextensions$getData`), and wires them up itself: a reload listener, the `neoforge:*` data map
 * payloads and configuration task, and mixins into Holder, MappedRegistry and TagLoader.
 *
 * With Nytheria Compat the real NeoForge classes win over those copies (see [ForeignNeoForgeClasses]), and Nytheria Compat's NeoForge
 * already loads, syncs and negotiates data maps (and tag removals). So the library's own implementation is switched
 * off (its initializers do nothing, its mixins run but nothing calls them), and its public API (`DataMapRegistry`) is routed to Nytheria Compat's NeoForge data maps:
 * - the Fabric (client) initializers do nothing,
 * - `FabricDataMapRegistry` registers through [dev.nytheria.compat.helpers.ForeignDataMapTypes] and reads the data
 *   with NeoForge's own `IWithData.getData`.
 */
object NeoForgeDataPackExtensionsCompat {
    const val MOD_ID = "neoforgedatapackextensions"
    private const val PACKAGE = "fuzs/neoforgedatapackextensions/"
    private const val METHOD_PREFIX = "neoforgedatapackextensions$"

    fun apply() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID))
            return

        NytheriaCompat.logger.info("Nytheria Compat: NeoForge Data Pack Extensions detected, its data maps are handled by Nytheria Compat's NeoForge")

        makeNoop("${PACKAGE}fabric/impl/NeoForgeDataPackExtensionsFabric", "onInitialize")
        makeNoop("${PACKAGE}fabric/impl/client/NeoForgeDataPackExtensionsFabricClient", "onInitializeClient")

        ClassTinkerers.addTransformation("${PACKAGE}fabric/impl/services/FabricDataMapRegistry") { classNode ->
            for (method in classNode.methods) {
                for (insn in method.instructions) {
                    if (insn !is MethodInsnNode)
                        continue

                    if (insn.owner == "net/neoforged/neoforge/registries/RegistryManager" && insn.name == "registerDataMap") {
                        insn.opcode = Opcodes.INVOKESTATIC
                        insn.owner = "dev/nytheria/compat/helpers/ForeignDataMapTypes"
                        insn.name = "register"
                        insn.itf = false
                    } else if (insn.name.startsWith(METHOD_PREFIX) && !insn.owner.startsWith(PACKAGE)) {
                        // holder.neoforgedatapackextensions$getData(type) -> NeoForge's holder.getData(type)
                        insn.name = insn.name.removePrefix(METHOD_PREFIX)
                    }
                }
            }
        }
    }

    private fun makeNoop(className: String, methodName: String) {
        ClassTinkerers.addTransformation(className) { classNode ->
            val method = classNode.methods.firstOrNull { it.name == methodName && it.desc == "()V" } ?: return@addTransformation

            method.instructions = InsnList().apply { add(InsnNode(Opcodes.RETURN)) }
            method.tryCatchBlocks?.clear()
            method.localVariables?.clear()
            method.maxStack = 0
            method.maxLocals = 1
        }
    }
}
