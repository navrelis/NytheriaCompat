/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.chunk;

import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

public interface RenderRegionCacheInjection {
    default void nytheriacompat$setNullForEmpty(boolean value) {
        throw NytheriaCompatHelper.createMixinException(RenderRegionCacheInjection.class, "nytheriacompat$setNullForEmpty");
    }

    default RenderChunkRegion createRegion(Level level, SectionPos pos, boolean nullForEmpty) {
        throw NytheriaCompatHelper.createMixinException(RenderRegionCacheInjection.class, "createRegion");
    }
}
