/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.inventory;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import dev.nytheria.compat.hook.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuInject {

    /*@Mixin(targets = "net.minecraft.world.inventory.EnchantmentMenu$2")
    public abstract static class AnonymousEnchantFuelSlotInject {
        @ModifyReturnValue(method = "mayPlace", at = @At("RETURN"))
        private boolean nytheriacompat$checkIsValidEnchantingFuel(boolean original, @Local(argsOnly = true) ItemStack stack) {
            return original || stack.is(Tags.Items.ENCHANTING_FUELS);
        }
    }*/

    @ModifyExpressionValue(method = "method_17411", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/EnchantingTableBlock;isValidBookShelf(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean nytheriacompat$addEnchantPowerBonus(boolean original, ItemStack itemStack, Level level, BlockPos blockPos, @Share("enchantPowerBonus") LocalFloatRef enchantPowerBonusRef, @Local(ordinal = 1) BlockPos pos2) {
        if (original) {
            // Nytheria Compat: other blocks count 1 each, as vanilla does.
            BlockState state = level.getBlockState(blockPos.offset(pos2));
            enchantPowerBonusRef.set(enchantPowerBonusRef.get() + (Scope.isNeoObject(state.getBlock()) ? state.getEnchantPowerBonus(level, blockPos.offset(pos2)) : 1));
        }

        return original;
    }

    @ModifyArg(method = "method_17411", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IILnet/minecraft/world/item/ItemStack;)I"), index = 1)
    private int nytheriacompat$useForgeEnchantBonus(int enchantBonus, @Share("enchantPowerBonus") LocalFloatRef enchantPowerBonusRef) {
        return (int) enchantPowerBonusRef.get();
    }

    // TODO: onPlayerEnchantItem

    /*@WrapOperation(method = "quickMoveStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z", ordinal = 0))
    private boolean nytheriacompat$checkIsEnchantingFuel(ItemStack instance, Item item, Operation<Boolean> original) {
        return original.call(instance, item) || instance.is(Tags.Items.ENCHANTING_FUELS);
    }*/
}
