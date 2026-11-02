// TRACKED HASH: 57e42230249e35464dc40d8cca2321d0e4608ae8
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.resources.model;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.*;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.model.geometry.GeometryLoaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
public class ModelManagerInject {
    @Shadow private Map<ModelResourceLocation, BakedModel> bakedRegistry;

    @Unique private ModelBakery modelBakery;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void nytheriacompat$initializeBakedRegistry(TextureManager textureManager, BlockColors blockColors, int i, CallbackInfo ci) {
        if (this.bakedRegistry == null)
            this.bakedRegistry = new HashMap<>();
    }

    @Inject(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;startTick()V", shift = At.Shift.AFTER))
    private void nytheriacompat$initGeometryLoader(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        GeometryLoaderManager.init();
    }

}