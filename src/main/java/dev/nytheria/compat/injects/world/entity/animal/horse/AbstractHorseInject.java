/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.animal.horse;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.world.entity.animal.horse.AbstractHorseInjection;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseInject extends Animal implements AbstractHorseInjection {
    @Shadow protected SimpleContainer inventory;

    protected AbstractHorseInject(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Definition(id = "travelVector", local = @Local(type = Vec3.class, ordinal = 0, argsOnly = true))
    @Definition(id = "z", field = "Lnet/minecraft/world/phys/Vec3;z:D")
    @Expression("travelVector.z > 0.0")
    @Inject(method = "executeRidersJump", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void nytheriacompat$callLivingJump(float playerJumpPendingScale, Vec3 travelVector, CallbackInfo ci) {
        CommonHooks.onLivingJump(this);
    }

    // Nytheria Compat: Don't implement the inject, rely on the accessor instead, because Better Combat has an accessor here that fails otherwise.
    /*public Container getInventory() {
        return this.inventory;
    }*/
}
