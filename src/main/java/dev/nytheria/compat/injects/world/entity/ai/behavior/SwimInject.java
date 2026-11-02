/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.ai.behavior;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Swim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Swim.class)
public abstract class SwimInject {
    @ModifyReturnValue(method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;)Z", at = @At("RETURN"))
    private boolean nytheriacompat$tryCheckFluidType(boolean original, @Local(argsOnly = true) Mob mob) {
        return original || (mob.isInFluidType() && mob.isInFluidType((fluidType, height) -> mob.canSwimInFluidType(fluidType) && height > mob.getFluidJumpThreshold()));
    }
}
