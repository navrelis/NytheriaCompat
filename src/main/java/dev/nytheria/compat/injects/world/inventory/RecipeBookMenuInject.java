// TRACKED HASH: 5e2075b943a16e07d906de887d9ca451cc35cc0b
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.world.inventory.RecipeBookMenuInjection;

import java.util.List;

@Mixin(RecipeBookMenu.class)
public abstract class RecipeBookMenuInject implements RecipeBookMenuInjection {
    @Shadow public abstract RecipeBookType getRecipeBookType();

    @Environment(EnvType.CLIENT)
    @Override
    public List<RecipeBookCategories> getRecipeBookCategories() {
        return RecipeBookCategories.getCategories(this.getRecipeBookType());
    }
}