/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.storage.loot;

import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface LootPoolInjection {
    default void freeze() {
        throw NytheriaCompatHelper.createMixinException(LootPoolInjection.class, "freeze");
    }

    default boolean isFrozen() {
        throw NytheriaCompatHelper.createMixinException(LootPoolInjection.class, "isFrozen");
    }

    default NumberProvider getRolls() {
        throw NytheriaCompatHelper.createMixinException(LootPoolInjection.class, "getRolls");
    }

    default NumberProvider getBonusRolls() {
        throw NytheriaCompatHelper.createMixinException(LootPoolInjection.class, "getBonusRolls");
    }

    default void setRolls(NumberProvider provider) {
        throw NytheriaCompatHelper.createMixinException(LootPoolInjection.class, "setRolls");
    }

    default void setBonusRolls(NumberProvider provider) {
        throw NytheriaCompatHelper.createMixinException(LootPoolInjection.class, "setBonusRolls");
    }
}
