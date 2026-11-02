/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.storage.loot;

import io.github.fabricators_of_create.porting_lib.loot.extensions.LootTableExtensions;
import dev.nytheria.compat.util.NytheriaCompatHelper;


public interface LootTableInjection extends LootTableExtensions {
    default void freeze() {
        throw NytheriaCompatHelper.createMixinException(LootTableInjection.class, "freeze");
    }

    default boolean isFrozen() {
        throw NytheriaCompatHelper.createMixinException(LootPoolInjection.class, "isFrozen");
    }

}
