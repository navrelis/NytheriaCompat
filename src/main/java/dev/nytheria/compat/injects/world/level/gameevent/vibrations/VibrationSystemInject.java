/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.gameevent.vibrations;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

@Mixin(VibrationSystem.class)
public interface VibrationSystemInject {
    // Nytheria Compat: NeoForge ships this data map for every vanilla game event, so the HEAD hooks replaced the vanilla method
    //  on every call. The vanilla body runs now and the data map value (same numbers unless a data pack changes them)
    //  is applied to its result.
    @ModifyReturnValue(method = "getGameEventFrequency(Lnet/minecraft/core/Holder;)I", at = @At("RETURN"))
    private static int nytheriacompat$tryUseDataMapIfPossible(int original, Holder<GameEvent> gameEvent) {
        var data = gameEvent.getData(NeoForgeDataMaps.VIBRATION_FREQUENCIES);
        return data != null ? data.frequency() : original;
    }

    @ModifyReturnValue(method = "getGameEventFrequency(Lnet/minecraft/resources/ResourceKey;)I", at = @At("RETURN"))
    private static int nytheriacompat$tryUseDataMapIfPossible(int original, ResourceKey<GameEvent> gameEvent) {
        var holder = BuiltInRegistries.GAME_EVENT.getHolder(gameEvent);

        if (holder.isPresent()) {
            var data = holder.orElseThrow().getData(NeoForgeDataMaps.VIBRATION_FREQUENCIES);

            if (data != null) {
                return data.frequency();
            }
        }

        return original;
    }
}
