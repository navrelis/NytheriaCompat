// TRACKED HASH: 2b151de67f98ea81974b31520a3345227de93180
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.client.renderer.RenderTypeInjection;

@Mixin(RenderType.class)
public abstract class RenderTypeInject implements RenderTypeInjection {
    @Unique
    private int chunkLayerId = -1;

    @Override
    public int getChunkLayerId() {
        RenderTypeInjection.nytheriacompat$initLoadedChunkLayers(); // Nytheria Compat: try to ensure chunk layers are actually all loaded.
        return chunkLayerId;
    }

    @Override
    public void setChunkLayerId(int id) {
        chunkLayerId = id;
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void nytheriacompat$initChunkLayerIds(CallbackInfo ci) {
        RenderTypeInjection.nytheriacompat$initLoadedChunkLayers();
    }
}