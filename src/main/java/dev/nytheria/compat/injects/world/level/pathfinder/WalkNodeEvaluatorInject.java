/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.pathfinder;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.world.level.pathfinder.PathfindingContextInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorInject {
    // Nytheria Compat: runs for every neighbour of every evaluated path node, resolve the override check only once.
    //  Asked against IBlockExtension, which declares getAdjacentBlockPathType as a default (vanilla's sweet berry bush / burning block rules); against Block.class it was true for every block.
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$ADJACENT_BLOCK_PATH_TYPE_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(IBlockExtension.class, "getAdjacentBlockPathType", BlockState.class, BlockGetter.class, BlockPos.class, Mob.class, PathType.class);

    @WrapOperation(method = "checkNeighbourBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/pathfinder/PathfindingContext;getPathTypeFromState(III)Lnet/minecraft/world/level/pathfinder/PathType;"))
    private static PathType nytheriacompat$tryGetAdjacentBlockPathType(PathfindingContext instance, int x, int y, int z, Operation<PathType> original, @Cancellable CallbackInfoReturnable<PathType> cir) {
        PathType pathType = original.call(instance, x, y, z);
        BlockPos currentEvalPos = instance.currentEvalPos();
        BlockState blockState = instance.level().getBlockState(currentEvalPos);

        if (nytheriacompat$ADJACENT_BLOCK_PATH_TYPE_OVERRIDE.test(blockState.getBlock().getClass())) {
            PathType blockPathType = blockState.getAdjacentBlockPathType(instance.level(), currentEvalPos, null, pathType);
            if (blockPathType != null) {
                cir.setReturnValue(blockPathType);
                return pathType;
            }
        }

        FluidState fluidState = blockState.getFluidState();
        PathType fluidPathType = fluidState.getAdjacentBlockPathType(instance.level(), currentEvalPos, null, pathType);
        if (fluidPathType != null) { // This replaces vanilla, should probably add a check for nytheria_compat stuff or something
            cir.setReturnValue(fluidPathType);
        }

        return pathType;
    }

    // please tell me this crash no longer occurs...
    /*@Inject(method = "getBlockPathTypeRaw", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", shift = At.Shift.AFTER), cancellable = true)
    private static void nytheriacompat$tryGetBlockPathType(BlockGetter level, BlockPos pos, CallbackInfoReturnable<BlockPathTypes> cir, @Local BlockState state) {
        try {
            var type = state.getBlockPathType(level, pos, null);
            if (type != null)
                cir.setReturnValue(type);
        } catch (NullPointerException ignored) {} // Nytheria Compat: The Forge deferred registry is going to be the fucking death of me. (crash only triggers w/ Lithium)
    }

    @Inject(method = "getBlockPathTypeRaw", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/BlockGetter;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;", shift = At.Shift.AFTER), cancellable = true)
    private static void nytheriacompat$tryGetFluidBlockPathType(BlockGetter level, BlockPos pos, CallbackInfoReturnable<BlockPathTypes> cir, @Local FluidState state) {
        try {
            var type = state.getBlockPathType(level, pos, null, false);
            if (type != null)
                cir.setReturnValue(type);
        } catch (NullPointerException ignored) {} // Nytheria Compat: The Forge deferred registry is going to be the fucking death of me. (crash only triggers w/ Lithium)
    }

    @Inject(method = "getBlockPathTypeRaw", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z", ordinal = 1), cancellable = true)
    private static void nytheriacompat$tryGetLoggableFluidBlockPathType(BlockGetter level, BlockPos pos, CallbackInfoReturnable<BlockPathTypes> cir, @Local FluidState state) {
        try {
            var type = state.getBlockPathType(level, pos, null, true);
            if (type != null)
                cir.setReturnValue(type);
        } catch (NullPointerException ignored) {} // Nytheria Compat: The Forge deferred registry is going to be the fucking death of me. (crash only triggers w/ Lithium)
    }*/
}
