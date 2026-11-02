// TRACKED HASH: 38ed9a92a3865ea4db1c28fa04a9e858ee73d618
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.texture;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.nytheria.compat.hook.Scope;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SpriteLoader.class)
public abstract class SpriteLoaderInject {
    @Shadow @Final private ResourceLocation location;

    // Nytheria Compat: a texture of a NeoForge mod must not lower the mip level of an atlas that holds the textures of every mod
    //  (Astral's 64x35 lucid_bell does it to the block atlas). If a texture of another mod lowers it too, vanilla's result stands.
    @WrapOperation(method = "stitch", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;log2(I)I", ordinal = 2), require = 0, expect = 0)
    private int nytheriacompat$avoidMipmapLowering(int value, Operation<Integer> original, @Local(argsOnly = true) int mipLevel, @Local(argsOnly = true) List<SpriteContents> sprites) {
        int lowered = original.call(value);
        if (lowered >= mipLevel) return lowered;
        int limit = 1 << mipLevel;
        for (SpriteContents sprite : sprites) {
            if (Scope.isNeoObject(sprite.name())) continue;
            limit = Math.min(limit, Math.min(Integer.lowestOneBit(sprite.width()), Integer.lowestOneBit(sprite.height())));
            limit = Math.min(limit, Math.min(sprite.width(), sprite.height()));
        }
        return limit < 1 << mipLevel ? lowered : mipLevel;
    }
}
