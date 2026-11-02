/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.world.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(CreativeModeTab.Builder.class)
public interface CreativeModeTabBuilderAccessor {
    @Accessor
    CreativeModeTab.Row getRow();

    @Accessor
    int getColumn();

    @Accessor
    Component getDisplayName();

    @Accessor
    Supplier<ItemStack> getIconGenerator();

    @Accessor
    CreativeModeTab.DisplayItemsGenerator getDisplayItemsGenerator();

    @Accessor
    boolean isCanScroll();

    @Accessor
    boolean isShowTitle();

    @Accessor
    boolean isAlignedRight();

    @Accessor
    CreativeModeTab.Type getType();
}
