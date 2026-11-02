/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.entity.player;


import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface PlayerInjection extends io.github.fabricators_of_create.porting_lib.blocks.injects.PlayerInjection, io.github.fabricators_of_create.porting_lib.entity.injects.PlayerInjection {
    String PERSISTED_NBT_TAG = "PlayerPersisted";

    default @Nullable Pose getForcedPose() {
        throw new IllegalStateException();
    }

    default float getDigSpeed(BlockState state, @Nullable BlockPos pos) {
        throw NytheriaCompatHelper.createMixinException(PlayerInjection.class, "getDigSpeed");
    }

    default boolean hasCorrectToolForDrops(BlockState state, Level level, BlockPos pos) {
        throw NytheriaCompatHelper.createMixinException(PlayerInjection.class, "hasCorrectToolForDrops");
    }
}
