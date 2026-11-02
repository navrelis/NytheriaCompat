/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import dev.nytheria.compat.injections.client.OptionsInjection;

@Mixin(Options.class)
public abstract class OptionsInject implements OptionsInjection {
    @Shadow @Final public KeyMapping keyAttack;

    @Shadow
    public abstract void load();

    @Unique private boolean nytheriacompat$loadOptionsLimited = false;

    @Override
    public void load(boolean limited) {
        this.nytheriacompat$loadOptionsLimited = limited;
        this.load();
        this.nytheriacompat$loadOptionsLimited = false;
    }

    @ModifyArg(method = "processOptions", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;getKey(Ljava/lang/String;)Lcom/mojang/blaze3d/platform/InputConstants$Key;"))
    private String nytheriacompat$tryReadModifierFromString(String name, @Local KeyMapping keyMapping) {
        if (name.indexOf(':') != -1) {
            var split = name.split(":");

            keyMapping.setKeyModifierAndCode(KeyModifier.valueFromString(split[1]), InputConstants.getKey(split[0]));
            return split[0];
        }

        return name;
    }

}
