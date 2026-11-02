// TRACKED HASH: 65326a80010482966a2797124eab63fbaf21f71e
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.biome;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.world.level.biome.BiomeInjection;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

@Mixin(Biome.class)
public abstract class BiomeInject implements BiomeInjection {
    private ModifiableBiomeInfo modifiableBiomeInfo;

    @Inject(at = @At("TAIL"), method = "<init>")
    public void nytheriacompat$loadModifiableBiomeInfo(Biome.ClimateSettings climateSettings, BiomeSpecialEffects biomeSpecialEffects, BiomeGenerationSettings biomeGenerationSettings, MobSpawnSettings mobSpawnSettings, CallbackInfo ci) {
        this.modifiableBiomeInfo = new ModifiableBiomeInfo(new ModifiableBiomeInfo.BiomeInfo(climateSettings, biomeSpecialEffects, biomeGenerationSettings, mobSpawnSettings));
    }

    @Override
    public ModifiableBiomeInfo modifiableBiomeInfo() {
        return this.modifiableBiomeInfo;
    }

    // Nytheria Compat: these replaced the vanilla getters at HEAD, so another mod's mixin into them never ran. The vanilla body
    //  returns the original settings; swap in the biome-modifier result only when nothing else changed the value.
    @ModifyReturnValue(at = @At("RETURN"), method = "getMobSettings")
    public MobSpawnSettings nytheriacompat$getModifiableMobSettings(MobSpawnSettings original) {
        return original == modifiableBiomeInfo().getOriginalBiomeInfo().mobSpawnSettings() ? modifiableBiomeInfo().get().mobSpawnSettings() : original;
    }

    @ModifyReturnValue(at = @At("RETURN"), method = "getGenerationSettings")
    public BiomeGenerationSettings nytheriacompat$getModifiableGenerationSettings(BiomeGenerationSettings original) {
        return original == modifiableBiomeInfo().getOriginalBiomeInfo().generationSettings() ? modifiableBiomeInfo().get().generationSettings() : original;
    }
}
