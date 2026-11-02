/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.neoforged.neoforge.items.VanillaInventoryCodeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DropperBlock.class)
public abstract class DropperBlockInject {
    @Definition(id = "itemStack", local = @Local(type = ItemStack.class, ordinal = 0))
    @Definition(id = "isEmpty", method = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z")
    @Expression("itemStack.isEmpty() == false")
    @ModifyExpressionValue(method = "dispenseFrom", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$callForgeDropperInsertHook(boolean original, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos pos, @Local DispenserBlockEntity blockEntity, @Local int slot, @Local ItemStack stack) {
        return original && VanillaInventoryCodeHooks.dropperInsertHook(level, pos, blockEntity, slot, stack);
    }
}
