/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item.crafting;

import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.ShapedRecipePatternStorage;
import dev.nytheria.compat.helpers.mixin.CreateStatic;

@Mixin(ShapedRecipePattern.class)
public class ShapedRecipePatternInject {
    private static int maxWidth = 3;
    private static int maxHeight = 3;

    @CreateStatic
    private static int getMaxWidth() {
        return ShapedRecipePatternStorage.getMaxWidth();
    }

    @CreateStatic
    private static int getMaxHeight() {
        return ShapedRecipePatternStorage.getMaxHeight();
    }

    @CreateStatic
    private static void setCraftingSize(int width, int height) {
        ShapedRecipePatternStorage.setCraftingSize(width, height);
        if (maxWidth < width) maxWidth = width;
        if (maxHeight < height) maxHeight = height;
    }
}
