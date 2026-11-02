/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.monster;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;

@Mixin(Witch.class)
public abstract class WitchInject extends Raider {
    protected WitchInject(EntityType<? extends Raider> entityType, Level level) {
        super(entityType, level);
    }

    @Definition(id = "damageAmount", local = @Local(type = float.class, argsOnly = true))
    @Expression("damageAmount = 0.0")
    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void nytheriacompat$addReductionToContainer(DamageSource damageSource, float damageAmount, CallbackInfoReturnable<Float> cir) {
        if (this.nytheriacompat$getDamageContainers().isEmpty()) return;
        this.nytheriacompat$getDamageContainers().peek().setReduction(DamageContainer.Reduction.INNATE_RESISTANCE, damageAmount);
    }
}
