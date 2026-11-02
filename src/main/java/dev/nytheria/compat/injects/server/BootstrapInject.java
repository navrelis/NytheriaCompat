// TRACKED HASH: 71cfb66f5af012480c2b566dde1de58adb0aeede
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.Bootstrap;
import net.neoforged.neoforge.registries.GameData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public class BootstrapInject {
    @Inject(method = "bootStrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTabs;validate()V", shift = At.Shift.AFTER))
    private static void nytheriacompat$initForgeHooks(CallbackInfo ci) {
        GameData.vanillaSnapshot();
    }

    @WrapWithCondition(method = "validate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/DefaultAttributes;validate()V"))
    private static boolean nytheriacompat$disableAttributeValidation() {
        return false;
    }
}