/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenEffectRenderer.class)
public interface ScreenEffectRendererAccessor {
    @Invoker
    static void callRenderWater(Minecraft minecraft, PoseStack poseStack) {
        throw new UnsupportedOperationException();
    }

    @Accessor("UNDERWATER_LOCATION")
    static ResourceLocation getUnderwaterLocation() {
        throw new UnsupportedOperationException();
    }
}
