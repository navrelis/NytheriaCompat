/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.mixin

import com.bawnorton.mixinsquared.api.MixinCanceller

class NytheriaCompatMixinCanceller : MixinCanceller {
    // Both are Porting Lib's own versions of what Nytheria Compat's NeoForge does, and they break it when they run too:
    private val cancelledMixins = listOf(
        // sets the loot table id a second time: "Attempted to rename loot table from 'minecraft:blocks/nether_wart_block' to ..." stops the datapack load
        "io.github.fabricators_of_create.porting_lib.loot.mixin.LootDataTypeMixin",
        // replaces the reload listener's registry lookup, so ContextAwareReloadListener.getRegistryLookup() is null in LootModifierManager.apply
        "io.github.fabricators_of_create.porting_lib.resources.mixin.common.SimplePreparableReloadListenerMixin",
    )

    override fun shouldCancel(targetClassNames: List<String>, mixinClassName: String): Boolean {
        return cancelledMixins.contains(mixinClassName)
    }
}
