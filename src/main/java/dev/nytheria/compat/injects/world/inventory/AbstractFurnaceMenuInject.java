/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.inventory;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceMenuInject {
    @Shadow @Final private RecipeType<? extends AbstractCookingRecipe> recipeType;

    @ModifyReturnValue(method = "isFuel", at = @At("RETURN"))
    private boolean nytheriacompat$checkIfIsFuel(boolean original, @Local(argsOnly = true) ItemStack stack) {
        return original || stack.getBurnTime(this.recipeType) > 0;
    }
}
