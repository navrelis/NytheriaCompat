// TRACKED HASH: 4b295056fcc0933cc21e852fd285a0e2f5c42bfe
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.block;

import java.util.List;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.client.renderer.block.ModelBlockRendererInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererInject implements ModelBlockRendererInjection {
    @Shadow public abstract void tesselateWithAO(BlockAndTintGetter blockAndTintGetter, BakedModel bakedModel, BlockState blockState, BlockPos blockPos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean bl, RandomSource randomSource, long l, int i);
    @Shadow public abstract void tesselateWithoutAO(BlockAndTintGetter blockAndTintGetter, BakedModel bakedModel, BlockState blockState, BlockPos blockPos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean bl, RandomSource randomSource, long l, int i);
    @Shadow public abstract void tesselateBlock(BlockAndTintGetter blockAndTintGetter, BakedModel bakedModel, BlockState blockState, BlockPos blockPos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean bl, RandomSource randomSource, long l, int i);
    @Shadow public abstract void renderModel(PoseStack.Pose pose, VertexConsumer vertexConsumer, @Nullable BlockState blockState, BakedModel bakedModel, float f, float g, float h, int i, int j);

    // Nytheria Compat: these run for every block and every quad list of a model that is tesselated, resolve the override checks only once.
    //  Asked against the extension interfaces, which declare these as defaults (against Block.class / BakedModel.class they were true for every
    //  class, or false because of a wrong parameter list: getLightEmission takes a BlockGetter, useAmbientOcclusion also takes the ModelData).
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$GET_LIGHT_EMISSION_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(IBlockExtension.class, "getLightEmission", BlockState.class, BlockGetter.class, BlockPos.class);
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$USE_AMBIENT_OCCLUSION_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(IBakedModelExtension.class, "useAmbientOcclusion", BlockState.class, ModelData.class, RenderType.class);
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$GET_QUADS_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(IBakedModelExtension.class, "getQuads", BlockState.class, Direction.class, RandomSource.class, ModelData.class, RenderType.class);

    @WrapOperation(method = "tesselateBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;useAmbientOcclusion()Z"))
    public boolean nytheriacompat$useForgeAmbientOcclusion(BakedModel instance, Operation<Boolean> original, @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) BlockAndTintGetter level, @Local(argsOnly = true) BlockPos pos) {
        if (nytheriacompat$USE_AMBIENT_OCCLUSION_OVERRIDE.test(instance.getClass())) {
            return Minecraft.useAmbientOcclusion() && switch (instance.useAmbientOcclusion(state, nytheriacompat$modelData.get(), nytheriacompat$renderType.get())) {
                case TRUE -> true;
                case DEFAULT -> state.getLightEmission(level, pos) == 0;
                case FALSE -> false;
            };
        }

        return original.call(instance);
    }

    @WrapOperation(method = {"tesselateWithAO", "tesselateWithoutAO", "renderModel"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;)Ljava/util/List;"))
    public List<BakedQuad> nytheriacompat$getQuadsWithAOOnForgeAtomics(BakedModel instance, @Nullable BlockState state, @Nullable Direction direction, RandomSource randomSource, Operation<List<BakedQuad>> original) {
        if (nytheriacompat$GET_QUADS_OVERRIDE.test(instance.getClass())) {
            return instance.getQuads(state, direction, randomSource, nytheriacompat$modelData.get(), nytheriacompat$renderType.get());
        }

        return original.call(instance, state, direction, randomSource);
    }

    @Inject(at = @At("RETURN"), method = "tesselateWithAO")
    public void nytheriacompat$resetAtomicsAfterAOTesselation(BlockAndTintGetter blockAndTintGetter, BakedModel bakedModel, BlockState blockState, BlockPos blockPos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean bl, RandomSource randomSource, long l, int i, CallbackInfo ci) {
        nytheriacompat$modelData.set(ModelData.EMPTY);
        nytheriacompat$renderType.set(null);
    }

    @Inject(at = @At("RETURN"), method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/resources/model/BakedModel;FFFII)V")
    public void nytheriacompat$resetAtomicsAfterModelRender(PoseStack.Pose pose, VertexConsumer vertexConsumer, BlockState blockState, BakedModel bakedModel, float f, float g, float h, int i, int j, CallbackInfo ci) {
        nytheriacompat$modelData.set(ModelData.EMPTY);
        nytheriacompat$renderType.set(null);
    }

    // Because we can't exactly provide new parameters easily, let's just do this.
    @Unique private final ThreadLocal<ModelData> nytheriacompat$modelData = ThreadLocal.withInitial(() -> ModelData.EMPTY);
    @Unique private final ThreadLocal<RenderType> nytheriacompat$renderType = new ThreadLocal<>();

    @Override
    public void tesselateWithAO(BlockAndTintGetter blockAndTintGetter, BakedModel bakedModel, BlockState blockState, BlockPos blockPos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean bl, RandomSource random, long l, int i, ModelData modelData, RenderType renderType) {
        nytheriacompat$modelData.set(modelData);
        nytheriacompat$renderType.set(renderType);
        tesselateWithAO(blockAndTintGetter, bakedModel, blockState, blockPos, poseStack, vertexConsumer, bl, random, l, i);
    }

    @Override
    public void tesselateWithoutAO(BlockAndTintGetter blockAndTintGetter, BakedModel bakedModel, BlockState blockState, BlockPos blockPos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean bl, RandomSource random, long l, int i, ModelData modelData, RenderType renderType) {
        nytheriacompat$modelData.set(modelData);
        nytheriacompat$renderType.set(renderType);
        tesselateWithoutAO(blockAndTintGetter, bakedModel, blockState, blockPos, poseStack, vertexConsumer, bl, random, l, i);
    }

    @Override
    public void tesselateBlock(BlockAndTintGetter blockAndTintGetter, BakedModel bakedModel, BlockState blockState, BlockPos blockPos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean bl, RandomSource random, long l, int i, ModelData modelData, RenderType renderType, boolean queryModelSpecificData) {
        nytheriacompat$renderType.set(renderType);
        if (queryModelSpecificData)
            modelData = bakedModel.getModelData(blockAndTintGetter, blockPos, blockState, modelData);
        nytheriacompat$modelData.set(modelData);

        tesselateBlock(blockAndTintGetter, bakedModel, blockState, blockPos, poseStack, vertexConsumer, bl, random, l, i);
    }

    @Override
    public void renderModel(PoseStack.Pose pose, VertexConsumer vertexConsumer, @Nullable BlockState blockState, BakedModel bakedModel, float f1, float f2, float f3, int i1, int i2, ModelData modelData, RenderType renderType) {
        nytheriacompat$modelData.set(modelData);
        nytheriacompat$renderType.set(renderType);
        renderModel(pose, vertexConsumer, blockState, bakedModel, f1, f2, f3, i1, i2);
    }

    // Sodium compatibility
    @IfModLoaded(value = "sodium", maxVersion = "0.6.0")
    @Dynamic
    @TargetHandler(
        mixin = "me.jellysquid.mods.sodium.mixin.features.render.model.block.BlockModelRendererMixin",
        name = "renderFast"
    )
    @WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;)Ljava/util/List;"), order = 1050)
    private List<BakedQuad> nytheriacompat$getForgeQuadsDirectional(BakedModel instance, BlockState state, Direction direction, RandomSource randomSource, Operation<List<BakedQuad>> original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(instance.getClass(), IBakedModelExtension.class, "getQuads", BlockState.class, Direction.class, RandomSource.class, ModelData.class, RenderType.class)) {
            return instance.getQuads(state, direction, randomSource, nytheriacompat$modelData.get(), nytheriacompat$renderType.get());
        }

        return original.call(instance, state, direction, randomSource);
    }
}
