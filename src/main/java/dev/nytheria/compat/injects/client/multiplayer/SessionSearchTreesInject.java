/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.multiplayer;

import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.CreativeModeTabSearchRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.injections.client.multiplayer.SessionSearchTreesInjection;

import java.util.List;

@Mixin(SessionSearchTrees.class)
public abstract class SessionSearchTreesInject implements SessionSearchTreesInjection {
    @Shadow public static SessionSearchTrees.Key CREATIVE_TAGS;
    @Shadow public abstract void updateCreativeTags(List<ItemStack> items);
    @Shadow public abstract void updateCreativeTooltips(HolderLookup.Provider registries, List<ItemStack> items);

    @Unique private SessionSearchTrees.Key nytheriacompat$key;

    @Override
    public void updateCreativeTags(List<ItemStack> items, SessionSearchTrees.Key key) {
        this.nytheriacompat$key = key;
        this.updateCreativeTags(items);
        this.nytheriacompat$key = null;
    }

    @Override
    public SearchTree<ItemStack> creativeTagSearch(SessionSearchTrees.Key key) {
        return CreativeModeTabSearchRegistry.getTagSearchTree(key).join();
    }

    @Override
    public void updateCreativeTooltips(HolderLookup.Provider provider, List<ItemStack> items, SessionSearchTrees.Key key) {
        this.nytheriacompat$key = key;
        this.updateCreativeTooltips(provider, items);
        this.nytheriacompat$key = null;
    }

    @Override
    public SearchTree<ItemStack> creativeNameSearch(SessionSearchTrees.Key key) {
        return CreativeModeTabSearchRegistry.getNameSearchTree(key).join();
    }
}
