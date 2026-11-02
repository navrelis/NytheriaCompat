/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.PumpkinBlock;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@Mixin(PumpkinBlock.class)
public abstract class PumpkinBlockInject {
    // Nytheria Compat: asked against IItemExtension, which declares canPerformAction as a default (false); against Item.class it was true for every item, so vanilla's shears check never ran.
    @ModifyExpressionValue(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean checkCanCarvePumpkin(boolean original, @Local(argsOnly = true) ItemStack stack) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(stack.getItem().getClass(), IItemExtension.class, "canPerformAction", ItemStack.class, ItemAbility.class)) {
            return stack.canPerformAction(ItemAbilities.SHEARS_CARVE);
        }
        return original;
    }
}
