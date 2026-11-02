/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.entity.raid;

import java.util.function.Supplier;

import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.raid.Raider;

public interface RaidRaiderTypeInjection {

    default Supplier<EntityType<? extends Raider>> nytheriacompat$getEntityTypeSupplier() {
        throw NytheriaCompatHelper.createMixinException(RaidRaiderTypeInjection.class, "nytheriacompat$getEntityTypeSupplier");
    }

    default boolean nytheriacompat$isNeo() {
        throw NytheriaCompatHelper.createMixinException(RaidRaiderTypeInjection.class, "nytheriacompat$isNeo");
    }
}
