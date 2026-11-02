/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.compat.sodium;

import java.util.List;

import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.fabric.model.FabricModelAccess;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.helpers.BakedModelOverrides;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

@IfModLoaded("sodium")
@Mixin(FabricModelAccess.class)
public abstract class FabricModelAccessMixin {
    // Nytheria Compat: Sodium's mesher asks these for every block, face and model on all of its worker threads, resolve the
    //  override checks only once.
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$GET_QUADS_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(IBakedModelExtension.class, "getQuads", BlockState.class, Direction.class, RandomSource.class, ModelData.class, RenderType.class);
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$GET_MODEL_DATA_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(IBakedModelExtension.class, "getModelData", BlockAndTintGetter.class, BlockPos.class, BlockState.class, ModelData.class);

    // Nytheria Compat: was a plain getRenderTypes override check against IBakedModelExtension, which is true for every
    //  SimpleBakedModel, MultiPartBakedModel and WeightedBakedModel (Nytheria Compat merges getRenderTypes into them), so Sodium's
    //  own layer was never used. Now only a model with NeoForge render types (a RenderTypeGroup, a real override, or a
    //  part/variant/wrapped model with one) or a block a NeoForge mod gave several layers takes NeoForge's answer (what
    //  Sodium's NeoForge build returns); any other model's NeoForge answer is ItemBlockRenderTypes' single layer, which
    //  is what Sodium's Fabric getModelRenderTypes returns itself.
    @Inject(method = "getModelRenderTypes", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$tryGetNeoRenderTypes(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, RandomSource random, SodiumModelData modelData, CallbackInfoReturnable<Iterable<RenderType>> cir) {
        if (BakedModelOverrides.usesNeoBlockRenderTypes(model, state)) {
            cir.setReturnValue(model.getRenderTypes(state, random, (ModelData) (Object) modelData));
        }
    }

    @Inject(method = "getQuads", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$tryGetNeoQuads(BlockAndTintGetter level, BlockPos pos, BakedModel model, BlockState state, Direction face, RandomSource random, RenderType renderType, SodiumModelData modelData, CallbackInfoReturnable<List<BakedQuad>> cir) {
        if (nytheriacompat$GET_QUADS_OVERRIDE.test(model.getClass()) && BakedModelOverrides.usesNeoQuads(model, state)) {
            cir.setReturnValue(model.getQuads(state, face, random, (ModelData) (Object) modelData, renderType));
        }
    }

    @Inject(method = "getModelData", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$tryGetNeoModelData(LevelSlice slice, BakedModel model, BlockState state, BlockPos pos, SodiumModelData originalData, CallbackInfoReturnable<SodiumModelData> cir) {
        if (nytheriacompat$GET_MODEL_DATA_OVERRIDE.test(model.getClass()) && BakedModelOverrides.usesNeoQuads(model, state)) {
            cir.setReturnValue((SodiumModelData) (Object) model.getModelData(slice, pos, state, (ModelData) (Object) originalData));
        }
    }

    // Nytheria Compat: Sodium's empty model data is this one object for every block; the casts in LevelSliceMixin need it to be a ModelData.
    @Inject(method = "getEmptyModelData", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$getNeoEmptyModelData(CallbackInfoReturnable<SodiumModelData> cir) {
        cir.setReturnValue((SodiumModelData) (Object) ModelData.EMPTY);
    }
}
