/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.chunk.status;

import java.util.EnumSet;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.world.level.chunk.status.ChunkStatusInjection;

import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.levelgen.Heightmap;

@Mixin(ChunkStatus.class)
public abstract class ChunkStatusInject implements ChunkStatusInjection {
    @Shadow @Final private EnumSet<Heightmap.Types> heightmapsAfter;
    @Shadow @Final private ChunkType chunkType;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nytheriacompat$fixMc308222(ChunkStatus parent, EnumSet heightmapsAfter, ChunkType chunkType, CallbackInfo ci) {
        EnumSet<Heightmap.Types> chunkSaveHeightmaps = EnumSet.copyOf(this.heightmapsAfter);
        if (this.chunkType != ChunkType.LEVELCHUNK) {
            chunkSaveHeightmaps.add(Heightmap.Types.WORLD_SURFACE_WG);
            chunkSaveHeightmaps.add(Heightmap.Types.OCEAN_FLOOR_WG);
        }

        this.chunkSaveHeightmaps = chunkSaveHeightmaps;
    }

    @Unique private EnumSet<Heightmap.Types> chunkSaveHeightmaps = EnumSet.noneOf(Heightmap.Types.class);

    @Override
    public EnumSet<Heightmap.Types> getChunkSaveHeightmaps() {
        return this.chunkSaveHeightmaps;
    }
}
