/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.RecipeBookCategories;

import java.util.List;

public interface RecipeBookMenuInjection {
    @Environment(EnvType.CLIENT)
    default List<RecipeBookCategories> getRecipeBookCategories() {
        throw new IllegalStateException();
    }
}
