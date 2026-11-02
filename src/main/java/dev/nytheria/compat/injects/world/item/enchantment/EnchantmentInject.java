/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item.enchantment;

import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.injections.world.item.enchantment.EnchantmentBuilderInjection;


@Mixin(Enchantment.class)
public abstract class EnchantmentInject {
    @Mixin(Enchantment.Builder.class)
    public abstract static class BuilderInject implements EnchantmentBuilderInjection {
    }
}
