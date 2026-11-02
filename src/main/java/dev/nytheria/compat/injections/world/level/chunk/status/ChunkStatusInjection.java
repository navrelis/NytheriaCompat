/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.chunk.status;

import java.util.EnumSet;

import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.world.level.levelgen.Heightmap;

public interface ChunkStatusInjection {
    default EnumSet<Heightmap.Types> getChunkSaveHeightmaps() {
        throw NytheriaCompatHelper.createMixinException(ChunkStatusInjection.class, "getChunkSaveHeightmaps");
    }
}
