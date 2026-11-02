/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.entity;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

public interface EntityTypeInjection<T extends Entity> {
    default void nytheriacompat$setVelocityUpdateSupplier(Predicate<EntityType<?>> supplier) {
        throw new IllegalStateException();
    }
    default void nytheriacompat$setTrackingRangeSupplier(ToIntFunction<EntityType<?>> supplier) {
        throw new IllegalStateException();
    }
    default void nytheriacompat$setUpdateIntervalSupplier(ToIntFunction<EntityType<?>> supplier) {
        throw new IllegalStateException();
    }

    default Stream<TagKey<EntityType<?>>> getTags() {
        throw new IllegalStateException();
    }
}
