/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.chunk;

import java.util.List;
import java.util.Map;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.helpers.BakedModelOverrides;
import dev.nytheria.compat.injections.client.renderer.chunk.SectionCompilerInjection;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerInject implements SectionCompilerInjection {
    @Shadow public abstract SectionCompiler.Results compile(SectionPos sectionPos, RenderChunkRegion region, VertexSorting vertexSorting, SectionBufferBuilderPack sectionBufferBuilderPack);
    @Shadow @Final private BlockRenderDispatcher blockRenderer;
    @Shadow protected abstract BufferBuilder getOrBeginLayer(Map<RenderType, BufferBuilder> bufferLayers, SectionBufferBuilderPack sectionBufferBuilderPack, RenderType renderType);

    @Unique private ThreadLocal<List<AddSectionGeometryEvent.AdditionalSectionRenderer>> nytheriacompat$additionalRenderers = ThreadLocal.withInitial(List::of);

    @Override
    public void nytheriacompat$setAdditionalRenderers(List<AddSectionGeometryEvent.AdditionalSectionRenderer> renderers) {
        this.nytheriacompat$additionalRenderers.set(renderers);
    }

    @Override
    public SectionCompiler.Results compile(SectionPos pos, RenderChunkRegion region, VertexSorting sorting, SectionBufferBuilderPack bufferBuilderPack, List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers) {
        try {
            this.nytheriacompat$additionalRenderers.set(additionalRenderers);
            return this.compile(pos, region, sorting, bufferBuilderPack);
        } finally {
            this.nytheriacompat$additionalRenderers.set(List.of());
        }
    }

    @Definition(id = "blockState", local = @Local(type = BlockState.class))
    @Definition(id = "getRenderShape", method = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;")
    @Definition(id = "MODEL", field = "Lnet/minecraft/world/level/block/RenderShape;MODEL:Lnet/minecraft/world/level/block/RenderShape;")
    @Expression("blockState.getRenderShape() == MODEL")
    @ModifyExpressionValue(method = "compile", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$handleModelDataCompile(boolean original, @Local BlockState state, @Local(argsOnly = true) RenderChunkRegion region, @Local(ordinal = 2) BlockPos pos, @Local RandomSource randomSource, @Local Map<RenderType, BufferBuilder> map, @Local(argsOnly = true) SectionBufferBuilderPack bufferBuilderPack, @Local PoseStack poseStack) {
        if (original) {
            var model = this.blockRenderer.getBlockModel(state);
            var modelData = region.getModelData(pos);
            modelData = model.getModelData(region, pos, state, modelData);
            randomSource.setSeed(state.getSeed(pos));

            // Nytheria Compat: Fallback to Vanilla. The check was a plain getRenderTypes override check against
            //  IBakedModelExtension, true for every SimpleBakedModel, MultiPartBakedModel and WeightedBakedModel (Nytheria Compat
            //  merges getRenderTypes into them), so no block ever took vanilla's path. A model without NeoForge render
            //  types (BakedModelOverrides) returns ItemBlockRenderTypes.getRenderLayers(state), and as long as the block has
            //  only one layer that is the vanilla chunk layer, so with no model data vanilla renders the same.
            if (modelData == ModelData.EMPTY && !BakedModelOverrides.usesNeoBlockRenderTypes(model, state)) {
                return true;
            }

            ChunkRenderTypeSet renderTypes = model.getRenderTypes(state, randomSource, modelData);

            for (RenderType renderType : renderTypes) {
                BufferBuilder bufferBuilder = this.getOrBeginLayer(map, bufferBuilderPack, renderType);
                poseStack.pushPose();
                poseStack.translate(SectionPos.sectionRelative(pos.getX()), SectionPos.sectionRelative(pos.getY()), SectionPos.sectionRelative(pos.getZ()));
                this.blockRenderer.renderBatched(state, pos, region, poseStack, bufferBuilder, true, randomSource, modelData, renderType);
                poseStack.popPose();
            }
        }

        return false;
    }

    @Inject(method = "compile", at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"))
    private void nytheriacompat$addAdditionalGeometryToCompiler(SectionPos sectionPos, RenderChunkRegion region, VertexSorting vertexSorting, SectionBufferBuilderPack sectionBufferBuilderPack, CallbackInfoReturnable<SectionCompiler.Results> cir, @Local Map<RenderType, BufferBuilder> map, @Local PoseStack poseStack) {
        ClientHooks.addAdditionalGeometry(this.nytheriacompat$additionalRenderers.get(), type -> this.getOrBeginLayer(map, sectionBufferBuilderPack, type), region, poseStack);
    }
}
