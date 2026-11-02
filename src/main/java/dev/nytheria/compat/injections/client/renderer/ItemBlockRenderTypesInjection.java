/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer;

import com.google.common.base.Preconditions;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.Util;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import dev.nytheria.compat.client.NytheriaCompatClient;
import dev.nytheria.compat.helpers.BakedModelOverrides;
import dev.nytheria.compat.mixin.ItemBlockRenderTypesAccessor;
import dev.nytheria.compat.util.DefaultedHashMap;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface ItemBlockRenderTypesInjection {
    ChunkRenderTypeSet CUTOUT_MIPPED = ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(RenderType.solid());
    Map<Block, ChunkRenderTypeSet> BLOCK_RENDER_TYPES = Util.make(new DefaultedHashMap<>(ItemBlockRenderTypes.TYPE_BY_BLOCK.size(), .5F), (it) -> {
        it.setDefaultValue(SOLID);
        ItemBlockRenderTypes.TYPE_BY_BLOCK.forEach((key, value) -> {
                it.put(key, ChunkRenderTypeSet.of(value));
        });
    });

    static ChunkRenderTypeSet getRenderLayers(BlockState state) {
        if (state.getBlock() instanceof LeavesBlock) {
            return ItemBlockRenderTypesAccessor.isRenderCutout() ? CUTOUT_MIPPED : SOLID;
        }

        // Nytheria Compat: Handle Fabric mods' render types
        if (!BLOCK_RENDER_TYPES.containsKey(state.getBlock())) {
            BLOCK_RENDER_TYPES.put(state.getBlock(), ChunkRenderTypeSet.of(ItemBlockRenderTypes.getChunkRenderType(state)));
        }

        return BLOCK_RENDER_TYPES.get(state.getBlock());
    }

    static void setRenderLayer(Block block, RenderType type) {
        setRenderLayer(block, ChunkRenderTypeSet.of(type));
    }

    static void setRenderLayer(Block block, Predicate<RenderType> predicate) {
        setRenderLayer(block, ChunkRenderTypeSet.of(RenderType.chunkBufferLayers().stream().filter(predicate).collect(Collectors.toList())));
    }

    static void setRenderLayer(Block block, ChunkRenderTypeSet layers) {
        checkClientLoading();
        BlockRenderLayerMap.INSTANCE.putBlock(block, layers.asList().get(0));
        BLOCK_RENDER_TYPES.put(block, layers);
        // Nytheria Compat: vanilla/Fabric only know the first layer (BlockRenderLayerMap above), so the render type hooks keep
        //  NeoForge's path for a block with several layers.
        BakedModelOverrides.onRenderLayersSet(block, layers);
    }

    static void setRenderLayer(Fluid fluid, RenderType type) {
        checkClientLoading();
        BlockRenderLayerMap.INSTANCE.putFluid(fluid, type);
    }

    static void checkClientLoading() {
        Preconditions.checkState(NytheriaCompatClient.Companion.getHasInitialized(), "Render layers can only be set during client loading!");
    }
}
