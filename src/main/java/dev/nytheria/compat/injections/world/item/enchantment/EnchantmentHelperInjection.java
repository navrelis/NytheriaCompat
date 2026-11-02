/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public interface EnchantmentHelperInjection {
    ThreadLocal<Boolean> nytheriacompat$shouldUseTagEnchantment = ThreadLocal.withInitial(() -> false);

    static int getTagEnchantmentLevel(Holder<Enchantment> enchantment, ItemStack stack) {
        nytheriacompat$shouldUseTagEnchantment.set(true);
        var value = EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
        nytheriacompat$shouldUseTagEnchantment.remove();
        return value;
    }
}
