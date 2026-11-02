/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

@Mixin(value = FarmBlock.class, priority = 1050)
public abstract class FarmBlockInject extends Block {
    public FarmBlockInject(Properties properties) {
        super(properties);
    }

    @WrapOperation(method = "isNearWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private static boolean nytheriacompat$checkCanBeHydrated(FluidState instance, TagKey<Fluid> tag, Operation<Boolean> original, @Local(argsOnly = true) LevelReader levelReader, @Local(argsOnly = true) BlockPos pos, @Local(ordinal = 1) BlockPos blockPos) {
        var state = levelReader.getBlockState(pos);
        return original.call(instance, tag) || state.canBeHydrated(levelReader, pos, instance, blockPos);
    }
}
