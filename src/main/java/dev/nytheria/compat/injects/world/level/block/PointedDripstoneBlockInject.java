/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.material.Fluid;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneBlockInject extends Block {
    public PointedDripstoneBlockInject(Properties properties) {
        super(properties);
    }

    @Definition(id = "randChance", local = @Local(type = float.class, ordinal = 0, argsOnly = true))
    @Expression("randChance > ?")
    @ModifyExpressionValue(method = "maybeTransferFluid", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean nytheriacompat$alwaysReturnTrue(boolean original) {
        return false;
    }

    @Definition(id = "fluid", local = @Local(type = Fluid.class))
    @Definition(id = "LAVA", field = "Lnet/minecraft/world/level/material/Fluids;LAVA:Lnet/minecraft/world/level/material/FlowingFluid;")
    @Expression("fluid != LAVA")
    @ModifyExpressionValue(method = "maybeTransferFluid", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean nytheriacompat$alwaysHandleIfNotLava(boolean original) {
        return false;
    }

    @Definition(id = "randChance", local = @Local(type = float.class, ordinal = 0, argsOnly = true))
    @Definition(id = "f", local = @Local(type = float.class, ordinal = 1))
    @Expression("randChance >= @(f)")
    @ModifyExpressionValue(method = "maybeTransferFluid", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static float nytheriacompat$tryHandleDripstoneChance(float original, @Local Fluid fluid) {
        var dripInfo = fluid.neo$getFluidType().getDripInfo();

        if (dripInfo != null)
            return dripInfo.chance();

        return original;
    }

    @ModifyVariable(method = "spawnDripParticle(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/Fluid;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private static ParticleOptions nytheriacompat$tryHandleCustomParticle(ParticleOptions options, @Cancellable CallbackInfo ci, @Local(ordinal = 1) Fluid fluid) {
        var dripInfo = fluid.neo$getFluidType().getDripInfo();
        if (dripInfo != null) {
            var particle = dripInfo.dripParticle();

            if (particle == null) {
                ci.cancel();
            }

            return particle;
        }

        return options;
    }

    @ModifyReturnValue(method = "canFillCauldron", at = @At("RETURN"))
    private static boolean nytheriacompat$checkHasDripInfo(boolean original, @Local(argsOnly = true) Fluid fluid) {
        return original || fluid.neo$getFluidType().getDripInfo() != null;
    }
}
