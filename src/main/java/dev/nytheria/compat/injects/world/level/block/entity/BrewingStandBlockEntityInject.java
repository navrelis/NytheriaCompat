/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block.entity;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityInject extends BaseContainerBlockEntity {
    protected BrewingStandBlockEntityInject(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // Nytheria Compat: getCraftingRemainingItem handled by Fabric API

    @Definition(id = "stack", local = @Local(type = ItemStack.class, argsOnly = true))
    @Definition(id = "is", method = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z")
    @Definition(id = "POTION", field = "Lnet/minecraft/world/item/Items;POTION:Lnet/minecraft/world/item/Item;")
    @Expression("stack.is(POTION)")
    @WrapOperation(method = "canPlaceItem", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$checkIsPotionInput(ItemStack instance, Item item, Operation<Boolean> original) {
        PotionBrewing brewing = this.level != null ? this.level.potionBrewing() : PotionBrewing.EMPTY;
        return brewing.isInput(instance) || original.call(instance, item);
    }
}
