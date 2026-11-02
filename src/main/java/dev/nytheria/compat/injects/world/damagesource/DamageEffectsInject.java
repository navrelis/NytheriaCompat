/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.damagesource;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageEffects;
import net.neoforged.fml.common.asm.enumextension.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;

import java.util.function.Supplier;

@NamedEnum
@NetworkedEnum(NetworkedEnum.NetworkCheck.CLIENTBOUND)
@Mixin(DamageEffects.class)
public abstract class DamageEffectsInject implements IExtensibleEnum {
    @Shadow @Final @Mutable private String id;
    @Shadow @Final @Mutable private SoundEvent sound;
    @Unique private Supplier<SoundEvent> soundSupplier;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nytheriacompat$initSoundSupplier(String string, int i, String id, SoundEvent sound, CallbackInfo ci) {
        this.soundSupplier = () -> sound;
    }

    @ReservedConstructor
    private DamageEffectsInject(String name, int ordinal, String id, SoundEvent sound) {}

    @CreateInitializer
    private DamageEffectsInject(String name, int ordinal, String id, Supplier<SoundEvent> soundSupplier) {
        this(name, ordinal, id, (SoundEvent) null);
        this.soundSupplier = soundSupplier;
        this.sound = null;
    }

    @CreateStatic
    private static ExtensionInfo getExtensionInfo() {
        return ExtensionInfo.nonExtended(DamageEffects.class);
    }
}
