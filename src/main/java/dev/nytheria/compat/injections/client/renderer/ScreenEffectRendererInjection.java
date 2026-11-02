/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import dev.nytheria.compat.mixin.ScreenEffectRendererAccessor;

import java.util.concurrent.atomic.AtomicReference;

public interface ScreenEffectRendererInjection {
    AtomicReference<ResourceLocation> currentTexture = new AtomicReference<>();

    static void renderFluid(Minecraft mc, PoseStack poseStack, ResourceLocation texture) {
        currentTexture.set(texture);
        ScreenEffectRendererAccessor.callRenderWater(mc, poseStack);
        currentTexture.set(ScreenEffectRendererAccessor.getUnderwaterLocation());
    }
}
