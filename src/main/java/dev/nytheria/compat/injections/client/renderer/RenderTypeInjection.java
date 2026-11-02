/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer;

import net.minecraft.client.renderer.RenderType;
import dev.nytheria.compat.processor.FabricInjectedInterface;

import java.util.concurrent.atomic.AtomicInteger;

@FabricInjectedInterface(RenderType.class)
public interface RenderTypeInjection {
    AtomicInteger nytheriacompat$loadedChunkLayers = new AtomicInteger(0);

    default int getChunkLayerId() {
        throw new RuntimeException("mixin.");
    }

    default void setChunkLayerId(int id) {
        throw new RuntimeException("mixin.");
    }

    static void nytheriacompat$initLoadedChunkLayers() {
        var layers = RenderType.chunkBufferLayers();
        if (layers.size() == nytheriacompat$loadedChunkLayers.get())
            return;

        var i = 0;
        for (var layer : layers)
            layer.setChunkLayerId(i++);

        nytheriacompat$loadedChunkLayers.set(layers.size());
    }
}
