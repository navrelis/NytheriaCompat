// TRACKED HASH: 2ea7001a7c90b80f52417cae8858b9c1cbe2be58
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.resources.model;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.helpers.BakedModelOverrides;

import java.util.List;

@Mixin(WeightedBakedModel.class)
public abstract class WeightedBakedModelInject implements IDynamicBakedModel, BakedModelOverrides.Composite {
    @Shadow public abstract List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random);

    @Shadow @Final private BakedModel wrapped;
    @Shadow @Final private List<WeightedEntry.Wrapper<BakedModel>> list;
    @Shadow @Final private int totalWeight;

    // Nytheria Compat: 0 = not resolved yet, 1 = no variant has NeoForge block render types, 2 = a variant has. The variants
    //  never change after baking, so this is resolved once; racing threads resolve the same value.
    @Unique private byte nytheriacompat$neoBlockRenderTypes;

    @Override
    public boolean nytheriacompat$hasNeoBlockRenderTypes() {
        byte resolved = this.nytheriacompat$neoBlockRenderTypes;

        if (resolved == 0) {
            resolved = 1;

            for (WeightedEntry.Wrapper<BakedModel> entry : this.list) {
                if (BakedModelOverrides.hasNeoBlockRenderTypes(entry.data())) {
                    resolved = 2;
                    break;
                }
            }

            this.nytheriacompat$neoBlockRenderTypes = resolved;
        }

        return resolved == 2;
    }

    // Nytheria Compat: vanilla's useAmbientOcclusion() and the NeoForge one below both answer from the wrapped (first) variant.
    @Override
    public BakedModel nytheriacompat$getAmbientOcclusionDelegate() {
        return this.wrapped;
    }
    @Unique private static final ThreadLocal<RenderType> nytheriacompat$renderType = ThreadLocal.withInitial(() -> null);
    @Unique private static final ThreadLocal<ModelData> nytheriacompat$modelData = ThreadLocal.withInitial(() -> ModelData.EMPTY);

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        nytheriacompat$renderType.set(renderType);
        nytheriacompat$modelData.set(extraData);

        var result = this.getQuads(state, side, rand);

        nytheriacompat$modelData.remove();
        nytheriacompat$renderType.remove();

        return result;
    }

    @WrapOperation(method = "method_33461", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;)Ljava/util/List;"))
    private static List<BakedQuad> nytheriacompat$useForgeQuadGetterIfAvailable(BakedModel instance, BlockState blockState, Direction direction, RandomSource randomSource, Operation<List<BakedQuad>> original) {
        var renderType = nytheriacompat$renderType.get();
        var modelData = nytheriacompat$modelData.get();

        if (renderType == null && modelData.equals(ModelData.EMPTY)) {
            // Defer back to original call, in case some other mods are relying on this behaviour.
            return original.call(instance, blockState, direction, randomSource);
        }

        return instance.getQuads(blockState, direction, randomSource, modelData, renderType);
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData modelData, RenderType renderType) {
        return this.wrapped.useAmbientOcclusion(state, modelData, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        return this.wrapped.getParticleIcon(data);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        return this.wrapped.applyTransform(transformType, poseStack, applyLeftHandTransform);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return WeightedRandom.getWeightedItem(this.list, Math.abs((int) rand.nextLong()) % this.totalWeight)
            .map(modelWrapper -> modelWrapper.data().getRenderTypes(state, rand, data))
            .orElse(ChunkRenderTypeSet.none());
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return this.wrapped.getModelData(level, pos, state, modelData);
    }
}