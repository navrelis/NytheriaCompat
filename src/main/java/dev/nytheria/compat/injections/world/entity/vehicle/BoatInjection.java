/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.entity.vehicle;

import java.util.function.Supplier;

import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.world.item.Item;

public interface BoatInjection {
    interface TypeInjection {
        default Item getSticks() {
            throw NytheriaCompatHelper.createMixinException(TypeInjection.class, "getSticks");
        }

        default boolean isRaft() {
            throw NytheriaCompatHelper.createMixinException(TypeInjection.class, "isRaft");
        }

        default void nytheriacompat$setRaft(boolean raft) {
            throw NytheriaCompatHelper.createMixinException(TypeInjection.class, "nytheriacompat$setRaft");
        }

        default Supplier<Item> nytheriacompat$getBoatItem() {
            throw NytheriaCompatHelper.createMixinException(TypeInjection.class, "nytheriacompat$getBoatItem");
        }

        default Supplier<Item> nytheriacompat$getChestBoatItem() {
            throw NytheriaCompatHelper.createMixinException(TypeInjection.class, "nytheriacompat$getChestBoatItem");
        }
    }
}
