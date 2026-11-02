// TRACKED HASH: 6e5ff0663e40cf957dd3b217b0541c32bd378ce0
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.blaze3d.vertex;

import java.nio.ByteBuffer;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.neoforge.client.extensions.IVertexConsumerExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(VertexConsumer.class)
public interface VertexConsumerInject extends IVertexConsumerExtension {
    @ModifyArg(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/FastColor$ARGB32;color(IIII)I"), index = 0)
    private int nytheriacompat$applyQuadAlpha(int originalAlpha, @Local(argsOnly = true) boolean readAlpha, @Local(argsOnly = true, ordinal = 3) float alpha, @Local ByteBuffer buffer) {
        return readAlpha ? (int) (((alpha * (float) (buffer.get(15) & 255)) / 255f) * 255) : originalAlpha;
    }

    @Definition(id = "l", local = @Local(type = int.class, ordinal = 4))
    @Definition(id = "lightmap", local = @Local(type = int[].class, ordinal = 0, argsOnly = true))
    @Expression("lightmap[l]")
    @ModifyExpressionValue(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V", at = @At("MIXINEXTRAS:EXPRESSION"))
    private int nytheriacompat$applyBakedLightingToBuffer(int original, @Local ByteBuffer buffer) {
        return applyBakedLighting(original, buffer);
    }
}