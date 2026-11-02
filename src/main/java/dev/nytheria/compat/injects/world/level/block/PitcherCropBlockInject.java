/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import dev.nytheria.compat.hook.Scope;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(PitcherCropBlock.class)
public abstract class PitcherCropBlockInject extends DoublePlantBlock {
    public PitcherCropBlockInject(Properties properties) {
        super(properties);
    }

    @Expression("false")
    @ModifyExpressionValue(method = "canSurvive", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$checkSoilDecisionWorks(boolean original, @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) LevelReader level, @Local(argsOnly = true) BlockPos pos) {
        var soil = level.getBlockState(pos.below());
        if (!Scope.isNeoObject((Object) soil.getBlock()))
            return original;

        var soilDecision = soil.canSustainPlant(level, pos.below(), Direction.UP, state);
        if (!soilDecision.isDefault()) {
            return soilDecision.isTrue();
        }

        return original;
    }
}
