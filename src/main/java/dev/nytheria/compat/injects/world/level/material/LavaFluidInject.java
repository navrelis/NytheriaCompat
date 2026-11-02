/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.material;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LavaFluid.class)
public abstract class LavaFluidInject {
    @Unique
    private final ThreadLocal<Direction> nytheriacompat$direction = new ThreadLocal<>();

    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/LavaFluid;isFlammable(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean nytheriacompat$setFlammableDirection(LavaFluid instance, LevelReader level, BlockPos pos, Operation<Boolean> original) {
        this.nytheriacompat$direction.set(Direction.UP);
        var result = original.call(instance, level, pos);
        this.nytheriacompat$direction.remove();
        return result;
    }

    @WrapOperation(method = "hasFlammableNeighbours", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/LavaFluid;isFlammable(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean nytheriacompat$setFlammableDirection(LavaFluid instance, LevelReader level, BlockPos pos, Operation<Boolean> original, @Local Direction direction) {
        this.nytheriacompat$direction.set(direction);
        var result = original.call(instance, level, pos);
        this.nytheriacompat$direction.remove();
        return result;
    }

    // Nytheria Compat: implemented *specifically* for better compatibility
    @WrapOperation(method = "isFlammable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;ignitedByLava()Z"))
    private boolean nytheriacompat$checkIsFlammableByDirection(BlockState instance, Operation<Boolean> original, @Local(argsOnly = true) LevelReader level, @Local(argsOnly = true) BlockPos pos) {
        return original.call(instance) || instance.isFlammable(level, pos, nytheriacompat$direction.get());
    }

    private boolean isFlammable(LevelReader level, BlockPos pos, Direction face) {
        return pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight() && !level.hasChunkAt(pos) ? false : level.getBlockState(pos).isFlammable(level, pos, face);
    }
}
