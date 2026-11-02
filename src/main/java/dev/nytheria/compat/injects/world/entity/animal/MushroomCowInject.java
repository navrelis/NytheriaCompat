/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.animal;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Nytheria Compat: This doesn't extend Cow for one reason and one reason only: https://github.com/FabricMC/Mixin/issues/196
@Mixin(MushroomCow.class)
public abstract class MushroomCowInject implements IShearable {
    // Nytheria Compat: Shearing handled by Porting Lib / Vanilla

    @Inject(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private void nytheriacompat$callLivingConvert(SoundSource source, CallbackInfo ci, @Local Cow cow) {
        EventHooks.onLivingConvert((LivingEntity) (Object) this, cow);
    }

    // TODO: captureDrops stuff
}
