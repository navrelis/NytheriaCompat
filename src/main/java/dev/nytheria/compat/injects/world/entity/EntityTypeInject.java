// TRACKED HASH: 285b05db3877c1ccdd76c7704e1de0c69adff005
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.world.entity.EntityTypeInjection;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

@Mixin(EntityType.class)
public abstract class EntityTypeInject<T extends Entity> implements EntityTypeInjection<T> {
    @SuppressWarnings("MixinAnnotationTarget") // this is supposed to exist, but I guess mixin doesn't think so?
    @Shadow @Nullable public abstract T create(Level level);
    @Shadow @Final private Holder.Reference<EntityType<?>> builtInRegistryHolder;
    @Shadow @Final private int clientTrackingRange;
    @Shadow @Final private int updateInterval;

    @Unique private Predicate<EntityType<?>> trackDeltasSupplier;
    @Unique private ToIntFunction<EntityType<?>> trackingRangeSupplier;
    @Unique private ToIntFunction<EntityType<?>> updateIntervalSupplier;

    @Override
    public void nytheriacompat$setVelocityUpdateSupplier(Predicate<EntityType<?>> supplier) {
        trackDeltasSupplier = supplier;
    }

    @Override
    public void nytheriacompat$setTrackingRangeSupplier(ToIntFunction<EntityType<?>> supplier) {
        trackingRangeSupplier = supplier;
    }

    @Override
    public void nytheriacompat$setUpdateIntervalSupplier(ToIntFunction<EntityType<?>> supplier) {
        updateIntervalSupplier = supplier;
    }

    @Override
    public Stream<TagKey<EntityType<?>>> getTags() {
        return this.builtInRegistryHolder.tags();
    }

    @Inject(at = @At("HEAD"), method = "clientTrackingRange", cancellable = true)
    public void nytheriacompat$useForgeTrackingRange(CallbackInfoReturnable<Integer> cir) {
        if (trackingRangeSupplier != null)
            cir.setReturnValue(trackingRangeSupplier.applyAsInt((EntityType<?>) (Object) this));
    }

    @Inject(at = @At("HEAD"), method = "updateInterval", cancellable = true)
    public void nytheriacompat$useForgeUpdateInterval(CallbackInfoReturnable<Integer> cir) {
        if (updateIntervalSupplier != null)
            cir.setReturnValue(updateIntervalSupplier.applyAsInt((EntityType<?>) (Object) this));
    }

    @Inject(at = @At("HEAD"), method = "trackDeltas", cancellable = true)
    public void nytheriacompat$useForgeVelocityUpdate(CallbackInfoReturnable<Boolean> cir) {
        if (trackDeltasSupplier != null)
            cir.setReturnValue(trackDeltasSupplier.test((EntityType<?>) (Object) this));
    }

    // TODO: any better ideas?
    private int defaultTrackingRangeSupplier() {
        return this.clientTrackingRange;
    }

    private int defaultUpdateIntervalSupplier() {
        return this.updateInterval;
    }

    private boolean defaultVelocitySupplier() {
        var self = (EntityType<?>) (Object) this;
        return self != EntityType.PLAYER && self != EntityType.LLAMA_SPIT && self != EntityType.WITHER && self != EntityType.BAT && self != EntityType.ITEM_FRAME && self != EntityType.GLOW_ITEM_FRAME && self != EntityType.LEASH_KNOT && self != EntityType.PAINTING && self != EntityType.END_CRYSTAL && self != EntityType.EVOKER_FANGS;
    }

    @Mixin(EntityType.Builder.class)
    public static class BuilderInject<T extends Entity> {
        private Predicate<EntityType<?>> velocityUpdateSupplier;
        private ToIntFunction<EntityType<?>> trackingRangeSupplier;
        private ToIntFunction<EntityType<?>> updateIntervalSupplier;

        // for all intents and purposes, mixin should add these methods in
        @Unique
        public EntityType.Builder<T> setUpdateInterval(int interval) {
            updateIntervalSupplier = t -> interval;
            return (EntityType.Builder<T>) (Object) this;
        }

        @Unique
        public EntityType.Builder<T> setTrackingRange(int range) {
            trackingRangeSupplier = t -> range;
            return (EntityType.Builder<T>) (Object) this;
        }

        @Unique
        public EntityType.Builder<T> setShouldReceiveVelocityUpdates(boolean value) {
            velocityUpdateSupplier = t -> value;
            return (EntityType.Builder<T>) (Object) this;
        }

        @Inject(at = @At("RETURN"), method = "build")
        public void nytheriacompat$addForgeBuilderItems(String string, CallbackInfoReturnable<EntityType<T>> cir) {
            var entityType = (EntityTypeInjection<T>) cir.getReturnValue();
            entityType.nytheriacompat$setTrackingRangeSupplier(trackingRangeSupplier);
            entityType.nytheriacompat$setUpdateIntervalSupplier(updateIntervalSupplier);
            entityType.nytheriacompat$setVelocityUpdateSupplier(velocityUpdateSupplier);
        }
    }

}