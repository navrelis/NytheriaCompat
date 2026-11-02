/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap

import net.fabricmc.loader.api.FabricLoader
import dev.nytheria.compat.api.remapping.InsnConflictRemapProvider

class NytheriaCompatInsnConflictRemapProvider : InsnConflictRemapProvider {
    private val mappingResolver = FabricLoader.getInstance().mappingResolver

    private val potionBrewingMapped = NytheriaCompatRemapper.remapClass("net/minecraft/world/item/alchemy/PotionBrewing\$Mix")
    private val pbFromMapped = mappingResolver.mapFieldName("intermediary", "net.minecraft.class_1845\$class_1846", "field_8962", "Ljava/lang/Object;")
    private val pbToMapped = mappingResolver.mapFieldName("intermediary", "net.minecraft.class_1845\$class_1846", "field_8961", "Ljava/lang/Object;")

    override fun remapMethod(owner: String, name: String, descriptor: String): String {
        when (owner) {
            "net/minecraftforge/fluids/FluidStack" ->
                when (name) {
                    "getAmount" -> return $$"forge$getAmount"
                    "writeToPacket" -> return $$"forge$writeToPacket"
                }

            "net/minecraft/world/effect/MobEffectInstance", NytheriaCompatRemapper.remapClass("net/minecraft/world/effect/MobEffectInstance") ->
                when (name) {
                    "getCures" -> return $$"neoforge$getCures"
                }

            "net/minecraft/server/level/ServerEntity", NytheriaCompatRemapper.remapClass("net/minecraft/server/level/ServerEntity") ->
                when (name) {
                    "sendPairingData" -> return $$"neoforge$sendPairingData"
                }

            "net/minecraft/world/item/alchemy/PotionBrewing", NytheriaCompatRemapper.remapClass("net/minecraft/world/item/alchemy/PotionBrewing") -> {
                when (name) {
                    "getRecipes" -> return $$"neoforge$getRecipes"
                }
            }
        }

        return super.remapMethod(owner, name, descriptor)
    }

    override fun remapField(owner: String, name: String, descriptor: String): String {
        return super.remapField(owner, name, descriptor)
    }
}
