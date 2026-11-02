/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import net.neoforged.neoforge.client.model.data.ModelData;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface RenderChunkRegionInjection {
    default void nytheriacompat$setModelDataSnapshot(Long2ObjectFunction<ModelData> modelDataSnapshot) {
        throw NytheriaCompatHelper.createMixinException(RenderChunkRegionInjection.class, "nytheriacompat$setModelDataSnapshot");
    }
}
