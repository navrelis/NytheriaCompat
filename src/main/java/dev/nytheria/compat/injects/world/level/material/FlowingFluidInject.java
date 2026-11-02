/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.material;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.nytheria.compat.hook.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidInject extends FluidInject {
    @Shadow protected abstract boolean canConvertToSource(Level level);

    // Nytheria Compat: vanilla and Fabric fluids keep their own answer (water and lava follow the game rules); a NeoForge fluid is asked through its fluid type.
    @WrapOperation(method = "getNewLiquid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;canConvertToSource(Lnet/minecraft/world/level/Level;)Z"))
    private boolean nytheriacompat$noopConvertToSource(FlowingFluid instance, Level level, Operation<Boolean> original, @Local(argsOnly = true) BlockPos pos) {
        if (!Scope.isNeoObject((Object) instance)) {
            return original.call(instance, level);
        }

        return instance.canConvertToSource(instance.getSource(false), level, pos);
    }

    @Override
    public boolean canConvertToSource(FluidState state, Level level, BlockPos pos) {
        return this.canConvertToSource(level);
    }
}
