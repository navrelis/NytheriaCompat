/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import dev.nytheria.compat.hook.Scope;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RedStoneWireBlock.class)
public abstract class RedStoneWireBlockInject extends Block {
    public RedStoneWireBlockInject(Properties properties) {
        super(properties);
    }

    @WrapOperation(method = "getConnectingSide(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/level/block/state/properties/RedstoneSide;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 0))
    private boolean nytheriacompat$checkCanRedstoneConnect(BlockState state, Operation<Boolean> original, @Local(ordinal = 1) BlockPos pos, @Local(argsOnly = true) BlockGetter level) {
        return original.call(state) || (Scope.isNeoObject((Object) state.getBlock()) && state.canRedstoneConnectTo(level, pos.above(), null));
    }

   @WrapOperation(method = "getConnectingSide(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/level/block/state/properties/RedstoneSide;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", ordinal = 0))
    private boolean nytheriacompat$checkCanRedstoneConnect(BlockState state, Direction direction, Operation<Boolean> original, @Local(argsOnly = true) BlockGetter level, @Local(ordinal = 1) BlockPos pos) {
       return original.call(state, direction) || (Scope.isNeoObject((Object) state.getBlock()) && state.canRedstoneConnectTo(level, pos, direction));
   }

    @WrapOperation(method = "getConnectingSide(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/level/block/state/properties/RedstoneSide;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 1))
    private boolean nytheriacompat$checkCanRedstoneConnectBelow(BlockState state, Operation<Boolean> original, @Local(ordinal = 1) BlockPos pos, @Local(argsOnly = true) BlockGetter level) {
        return original.call(state) || (Scope.isNeoObject((Object) state.getBlock()) && state.canRedstoneConnectTo(level, pos.below(), null));
    }
}
