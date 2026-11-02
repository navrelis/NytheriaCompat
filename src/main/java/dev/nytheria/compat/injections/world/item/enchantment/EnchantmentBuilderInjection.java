/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item.enchantment;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.function.UnaryOperator;

public interface EnchantmentBuilderInjection {
    default Enchantment.Builder withCustomName(UnaryOperator<MutableComponent> nameFactory) {
        throw NytheriaCompatHelper.createMixinException(EnchantmentBuilderInjection.class, "withCustomName");
    }
}
