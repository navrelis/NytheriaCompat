/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.multiplayer;

import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.List;

public interface SessionSearchTreesInjection {
    default void updateCreativeTags(List<ItemStack> items, SessionSearchTrees.Key key) {
        throw NytheriaCompatHelper.createMixinException(SessionSearchTreesInjection.class, "updateCreativeTags");
    }

    default SearchTree<ItemStack> creativeTagSearch(SessionSearchTrees.Key key) {
        throw NytheriaCompatHelper.createMixinException(SessionSearchTreesInjection.class, "creativeTagSearch");
    }

    default void updateCreativeTooltips(HolderLookup.Provider provider, List<ItemStack> items, SessionSearchTrees.Key key) {
        throw NytheriaCompatHelper.createMixinException(SessionSearchTreesInjection.class, "updateCreativeTooltips");
    }

    default SearchTree<ItemStack> creativeNameSearch(SessionSearchTrees.Key key) {
        throw NytheriaCompatHelper.createMixinException(SessionSearchTreesInjection.class, "creativeNameSearch");
    }
}
