/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import dev.nytheria.compat.hook.Scope;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(MangrovePropaguleBlock.class)
public abstract class MangrovePropaguleBlockInject extends SaplingBlock {
    public MangrovePropaguleBlockInject(TreeGrower treeGrower, Properties properties) {
        super(treeGrower, properties);
    }

    @Definition(id = "level", local = @Local(type = LevelReader.class, argsOnly = true))
    @Definition(id = "getBlockState", method = "Lnet/minecraft/world/level/LevelReader;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;")
    @Definition(id = "pos", local = @Local(type = BlockPos.class, argsOnly = true))
    @Definition(id = "above", method = "Lnet/minecraft/core/BlockPos;above()Lnet/minecraft/core/BlockPos;")
    @Expression("level.getBlockState(pos.above())")
    @WrapOperation(method = "canSurvive", at = @At("MIXINEXTRAS:EXPRESSION"))
    private BlockState nytheriacompat$checkSoilDecision(LevelReader instance, BlockPos pos, Operation<BlockState> original, @Cancellable CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) BlockState plantState) {
        var state = original.call(instance, pos);
        if (!Scope.isNeoObject((Object) state.getBlock()))
            return state;

        var soilDecision = state.canSustainPlant(instance, pos, Direction.DOWN, plantState);
        if (!soilDecision.isDefault()) {
            cir.setReturnValue(soilDecision.isTrue());
        }

        return state;
    }
}
