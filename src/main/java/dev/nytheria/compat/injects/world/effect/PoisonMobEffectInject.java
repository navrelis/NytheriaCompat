/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.effect;

import com.llamalad7.mixinextras.sugar.Local;
import dev.nytheria.compat.hook.Scope;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net/minecraft/world/effect/PoisonMobEffect")
public abstract class PoisonMobEffectInject {
    @ModifyArg(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private DamageSource nytheriacompat$useNeoPoisonDamage(DamageSource damageSource, @Local(argsOnly = true) LivingEntity entity) {
        var dTypeReg = entity.damageSources().damageTypes;
        var dType = dTypeReg.getHolder(NeoForgeMod.POISON_DAMAGE);

        // Nytheria Compat: Fabric mods check for magic here, only Astral's entities read NeoForge's poison type.
        if (!Scope.isNeoObject(entity) || !damageSource.is(DamageTypes.MAGIC) || dType.isEmpty()) {
            return damageSource;
        }

        return new DamageSource(dType.orElseThrow());
    }
}
