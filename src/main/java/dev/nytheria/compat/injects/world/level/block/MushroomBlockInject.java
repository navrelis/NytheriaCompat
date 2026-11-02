/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;


import dev.nytheria.compat.hook.Scope;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(MushroomBlock.class)
public abstract class MushroomBlockInject extends BushBlock {
    protected MushroomBlockInject(Properties properties) {
        super(properties);
    }

    @Inject(method = "canSurvive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"), cancellable = true)
    private void nytheriacompat$handleSoilDecision(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) BlockState soilState, @Local(ordinal = 1) BlockPos soilPos) {
        if (!Scope.isNeoObject((Object) soilState.getBlock()))
            return;

        var soilDecision = soilState.canSustainPlant(level, soilPos, Direction.UP, state);

        if (!soilDecision.isDefault()) {
            cir.setReturnValue(soilDecision.isTrue());
        }
    }

}
