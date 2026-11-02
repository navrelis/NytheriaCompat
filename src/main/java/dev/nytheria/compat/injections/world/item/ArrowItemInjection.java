/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface ArrowItemInjection {
    default boolean isInfinite(ItemStack stack, ItemStack bow, LivingEntity entity) {
        throw NytheriaCompatHelper.createMixinException(ArrowItemInjection.class, "isInfinite");
    }
}
