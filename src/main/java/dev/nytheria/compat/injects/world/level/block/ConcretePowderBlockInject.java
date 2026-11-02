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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConcretePowderBlock.class)
public abstract class ConcretePowderBlockInject {
    @WrapOperation(method = "shouldSolidify", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/ConcretePowderBlock;canSolidify(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean nytheriacompat$checkBlockCanBeHydrated(BlockState state, Operation<Boolean> original, @Local(argsOnly = true) BlockGetter level, @Local(argsOnly = true) BlockPos pos) {
        return state.canBeHydrated(level, pos, state.getFluidState(), pos) || original.call(state);
    }

    @Inject(method = "touchesLiquid", at = @At("HEAD"))
    private static void nytheriacompat$storeBlockState(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir, @Share("state") LocalRef<BlockState> stateRef) {
        stateRef.set(level.getBlockState(pos));
    }

    @WrapOperation(method = "touchesLiquid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/ConcretePowderBlock;canSolidify(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean nytheriacompat$checkBlockCanBeHydrated(BlockState state, Operation<Boolean> original, @Local(argsOnly = true) BlockGetter level, @Local(argsOnly = true) BlockPos pos, @Local BlockPos.MutableBlockPos pos2, @Share("state") LocalRef<BlockState> stateRef) {
        return stateRef.get().canBeHydrated(level, pos, state.getFluidState(), pos2) || original.call(state);
    }
}
