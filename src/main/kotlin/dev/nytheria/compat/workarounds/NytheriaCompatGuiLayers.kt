/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.workarounds

import net.minecraft.resources.ResourceLocation
import dev.nytheria.compat.NytheriaCompat

object NytheriaCompatGuiLayers {
    @JvmField val HOTBAR_AND_DECORATIONS: ResourceLocation = ResourceLocation.fromNamespaceAndPath(NytheriaCompat.MOD_ID, "hotbar_and_decorations")
}
