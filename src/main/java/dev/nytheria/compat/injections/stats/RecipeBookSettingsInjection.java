/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.stats;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.inventory.RecipeBookType;
import dev.nytheria.compat.mixin.RecipeBookSettingsAccessor;

public interface RecipeBookSettingsInjection {
    static void addTagsForType(RecipeBookType type, String openTag, String filteringTag) {
        RecipeBookSettingsAccessor.getTagFields().put(type, new Pair<>(openTag, filteringTag));
    }
}
