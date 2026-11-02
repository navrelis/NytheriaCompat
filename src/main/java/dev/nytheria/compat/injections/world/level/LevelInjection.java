/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import io.github.fabricators_of_create.porting_lib.extensions.common.LevelExtensions;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public interface LevelInjection {
    default ArrayList<BlockSnapshot> nytheriacompat$getCapturedBlockSnapshots() {
        throw new IllegalStateException();
    }

    default boolean nytheriacompat$getRestoringBlockSnapshots() {
        throw NytheriaCompatHelper.createMixinException(LevelInjection.class, "nytheriacompat$getRestoringBlockSnapshots");
    }

    default boolean nytheriacompat$getCapturingBlockSnapshots() {
        throw NytheriaCompatHelper.createMixinException(LevelInjection.class, "nytheriacompat$getCapturingBlockSnapshots");
    }

    default void nytheriacompat$setCapturingBlockSnapshots(boolean value) {
        throw NytheriaCompatHelper.createMixinException(LevelInjection.class, "nytheriacompat$setCapturingBlockSnapshots");
    }

    default void nytheriacompat$setRestoringBlockSnapshots(boolean value) {
        throw NytheriaCompatHelper.createMixinException(LevelInjection.class, "nytheriacompat$setRestoringBlockSnapshots");
    }

    default void setDayTimeFraction(float dayTimeFraction) {
        throw NytheriaCompatHelper.createMixinException(LevelInjection.class, "setDayTimeFraction");
    }

    default float getDayTimeFraction() {
        throw NytheriaCompatHelper.createMixinException(LevelInjection.class, "getDayTimeFraction");
    }

    default float getDayTimePerTick() {
        throw NytheriaCompatHelper.createMixinException(LevelInjection.class, "getDayTimePerTick");
    }

    default void setDayTimePerTick(float dayTimePerTick) {
        throw NytheriaCompatHelper.createMixinException(LevelInjection.class, "setDayTimePerTick");
    }

    default long advanceDaytime() {
        throw NytheriaCompatHelper.createMixinException(LevelInjection.class, "advanceDaytime");
    }

    default void markAndNotifyBlock(BlockPos pos, @Nullable LevelChunk levelchunk, BlockState oldState, BlockState newState, int flags, int p_46608_) {
        ((LevelExtensions) this).port_lib$markAndNotifyBlock(pos, levelchunk, oldState, newState, flags, p_46608_);
    }

    default void addFreshBlockEntities(Collection<BlockEntity> list) {
        throw NytheriaCompatHelper.createMixinException(LevelInjection.class, "addFreshBlockEntities");
    }
}
