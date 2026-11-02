/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.function.Predicate;

public interface ProjectileWeaponItemInjection {
    default Predicate<ItemStack> getSupportedHeldProjectiles(ItemStack stack) {
        throw NytheriaCompatHelper.createMixinException(ProjectileWeaponItemInjection.class, "getSupportedHeldProjectiles");
    }

    default Predicate<ItemStack> getAllSupportedProjectiles(ItemStack stack) {
        throw NytheriaCompatHelper.createMixinException(ProjectileWeaponItemInjection.class, "getAllSupportedProjectiles");
    }

    default AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        throw NytheriaCompatHelper.createMixinException(ProjectileWeaponItemInjection.class, "customArrow");
    }

    default ItemStack getDefaultCreativeAmmo(@Nullable Player player, ItemStack projectileWeaponItem) {
        throw NytheriaCompatHelper.createMixinException(ProjectileWeaponItemInjection.class, "getDefaultCreativeAmmo");
    }
}
