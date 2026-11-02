// TRACKED HASH: 556f112336560cecf132e4932932da41a78fc263
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CoralBlock.class)
public abstract class CoralBlockInject {
    @Inject(method = "scanForWater", at = @At("HEAD"))
    private void nytheriacompat$getBlockStateAtPos(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir, @Share("state") LocalRef<BlockState> state) {
        state.set(level.getBlockState(pos));
    }

    @WrapOperation(method = "scanForWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean nytheriacompat$checkCanHydrate(FluidState instance, TagKey<Fluid> tag, Operation<Boolean> original, @Local(argsOnly = true) BlockGetter level, @Local(argsOnly = true) BlockPos pos, @Share("state") LocalRef<BlockState> state, @Local Direction direction) {
        return state.get().canBeHydrated(level, pos, instance, pos.relative(direction)) || original.call(instance, tag);
    }
}