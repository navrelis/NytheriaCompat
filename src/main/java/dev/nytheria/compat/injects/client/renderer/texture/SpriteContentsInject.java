// TRACKED HASH: db725119dbf943686f97f078ada41b52bcc6aef3
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.texture;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(SpriteContents.class)
public abstract class SpriteContentsInject {
    @Shadow @Final int width;
    @Shadow @Final int height;

    @Inject(method = "upload", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;upload(IIIIIIIZZ)V", ordinal = 0, shift = At.Shift.BEFORE), cancellable = true)
    private void nytheriacompat$skipUploadIfInvalidMipLevel(CallbackInfo ci, @Local(ordinal = 4) int i) {
        if ((this.width >> i) <= 0 || (this.height >> i) <= 0)
            ci.cancel();
    }

    @Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$InterpolationData")
    public static class InterpolationDataInject {
        @ModifyArgs(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;<init>(IIZ)V"))
        private void nytheriacompat$guardFromInvalidTexSize(Args args) {
            args.set(0, Math.max(1, args.get(0)));
            args.set(1, Math.max(1, args.get(1)));
        }
    }
}