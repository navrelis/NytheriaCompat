/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.multiplayer.prediction;

import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.client.multiplayer.prediction.BlockStatePredictionHandlerInjection;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.core.BlockPos;

@Mixin(BlockStatePredictionHandler.class)
public abstract class BlockStatePredictionHandlerInject implements BlockStatePredictionHandlerInjection {

    @Inject(method = "endPredictionsUpTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;syncBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void nytheriacompat$restoreBlockEntityIfCancelled(int sequence, ClientLevel level, CallbackInfo ci, @Local BlockStatePredictionHandler.ServerVerifiedState verifiedState, @Local BlockPos pos) {
        if (verifiedState.nytheriacompat$getSnapshot() != null && verifiedState.blockState == verifiedState.nytheriacompat$getSnapshot().getState()) {
            if (verifiedState.nytheriacompat$getSnapshot().restoreBlockEntity(level, pos)) {
                level.sendBlockUpdated(pos, verifiedState.blockState, verifiedState.blockState, 3);
            }
        }
    }

    @Mixin(BlockStatePredictionHandler.ServerVerifiedState.class)
    public abstract static class ServerVerifiedStateInject implements ServerVerifiedStateInjection {
        @Unique BlockSnapshot snapshot;

        @Override
        public BlockSnapshot nytheriacompat$getSnapshot() {
            return this.snapshot;
        }

    }
}
