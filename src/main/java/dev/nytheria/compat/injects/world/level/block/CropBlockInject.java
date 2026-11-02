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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.level.block.CropBlockInjection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(CropBlock.class)
public abstract class CropBlockInject extends BushBlock implements CropBlockInjection {
    @Shadow
    protected static float getGrowthSpeed(Block block, BlockGetter level, BlockPos pos) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    protected CropBlockInject(Properties properties) {
        super(properties);
    }

    @Unique private static final ThreadLocal<BlockState> nytheriacompat$currentState = CropBlockInjection.nytheriacompat$currentState;

    @CreateStatic
    private static float getGrowthSpeed(BlockState state, BlockGetter level, BlockPos pos) {
        nytheriacompat$currentState.set(state);
        float value = getGrowthSpeed(state.getBlock(), level, pos);
        nytheriacompat$currentState.remove();
        return value;
    }

    @WrapOperation(method = "getGrowthSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z", ordinal = 0))
    private static boolean nytheriacompat$checkCanSustainPlant(BlockState instance, Block block, Operation<Boolean> original, @Local(argsOnly = true) BlockGetter level, @Local(ordinal = 1) BlockPos pos, @Local(ordinal = 0) int i, @Local(ordinal = 1) int j, @Local(argsOnly = true) Block block2) {
        var state = nytheriacompat$currentState.get() != null ? nytheriacompat$currentState.get() : block2.defaultBlockState();
        if (!Scope.isNeoObject((Object) instance.getBlock()))
            return original.call(instance, block);

        var soilDecision = instance.canSustainPlant(level, pos.offset(i, 0, j), Direction.UP, state);

        if (!soilDecision.isDefault())
            return soilDecision.isTrue();

        return original.call(instance, block) || instance.getBlock() instanceof FarmBlock;
    }

}
