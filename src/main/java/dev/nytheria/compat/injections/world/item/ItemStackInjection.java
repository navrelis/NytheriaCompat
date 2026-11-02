/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public interface ItemStackInjection {
    default void hurtAndBreak(int damage, ServerLevel level, @Nullable LivingEntity entity, Consumer<Item> onBreak) {
        throw NytheriaCompatHelper.createMixinException(ItemStackInjection.class, "hurtAndBreak");
    }

    default ItemEnchantments getTagEnchantments() {
        throw NytheriaCompatHelper.createMixinException(ItemStackInjection.class, "getTagEnchantments");
    }
}
