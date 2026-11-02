/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.workarounds

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.material.Fluid
import dev.nytheria.compat.NytheriaCompat

object NytheriaCompatFluidTags {
    @JvmField val EMPTY: TagKey<Fluid> = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(NytheriaCompat.MOD_ID, "empty"))
    @JvmField val EMPTY_NONVANILLA: TagKey<Fluid> = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(NytheriaCompat.MOD_ID, "empty_nonvanilla"))

    @JvmStatic
    fun isTagForFluidTypePushing(tag: TagKey<Fluid>): Boolean {
        return tag == EMPTY || tag == EMPTY_NONVANILLA
    }
}
