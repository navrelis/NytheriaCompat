/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import dev.nytheria.compat.hook.Scope;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

@Mixin(SugarCaneBlock.class)
public abstract class SugarCaneBlockInject extends Block {
    public SugarCaneBlockInject(Properties properties) {
        super(properties);
    }

    @Inject(method = "canSurvive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z", ordinal = 0), cancellable = true)
    private void nytheriacompat$checkCanSustainPlant(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) BlockState soilState) {
        var soil = level.getBlockState(pos.below());
        if (!Scope.isNeoObject((Object) soil.getBlock()))
            return;

        var soilDecision = soil.canSustainPlant(level, pos.below(), Direction.UP, state);
        if (!soilDecision.isDefault())
            cir.setReturnValue(soilDecision.isTrue());
    }

    @WrapOperation(method = "canSurvive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z", ordinal = 0))
    private boolean nytheriacompat$checkCanBeHydrated(FluidState instance, TagKey<Fluid> tag, Operation<Boolean> original, BlockState state, LevelReader level, BlockPos pos, @Local Direction direction) {
        return original.call(instance, tag) || state.canBeHydrated(level, pos, instance, pos.relative(direction));
    }
}
