/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.neoforged.neoforge.common.extensions.IFluidExtension;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@Mixin(ExplosionDamageCalculator.class)
public abstract class ExplosionDamageCalculatorInject {
    @WrapOperation(method = "getBlockExplosionResistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getExplosionResistance()F"))
    private float nytheriacompat$tryUseForgeExplosionResistance(FluidState instance, Operation<Float> original, @Local(argsOnly = true) BlockGetter level, @Local(argsOnly = true) Explosion explosion, @Local(argsOnly = true) BlockPos pos) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(instance.getType().getClass(), IFluidExtension.class, "getExplosionResistance", FluidState.class, BlockGetter.class, BlockPos.class, Explosion.class)) {
            return instance.getExplosionResistance(level, pos, explosion);
        }

        return original.call(instance);
    }
}
