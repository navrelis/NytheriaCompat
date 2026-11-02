/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item;

import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface DyeColorInjection {
    static DyeColor getColor(ItemStack stack) {
        if (stack.getItem() instanceof DyeItem)
            return ((DyeItem) stack.getItem()).getDyeColor();

        for (int x = 0; x < DyeColor.BLACK.getId(); x++) {
            var dyeColor = DyeColor.byId(x);
            if (stack.is(dyeColor.getTag()))
                return dyeColor;
        }

        return null;
    }

    default TagKey<Item> getTag() {
        throw NytheriaCompatHelper.createMixinException(DyeColorInjection.class, "getTag");
    }

}
