// TRACKED HASH: 66d460a5ca67d57026ea83d7b1223b548ab9de3d
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.client.renderer.LevelRendererInjection;
import dev.nytheria.compat.util.IteratorWrapper;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

// higher priority to allow for Sodium to overwrite the method
@Mixin(value = LevelRenderer.class, priority = 1050)
@Implements(@Interface(iface = LevelRendererInjection.class, prefix = "nytheriacompat$i$"))
public abstract class LevelRendererInject {
    @Shadow private int ticks;
    @Shadow @Nullable private Frustum capturedFrustum;
    @Shadow private Frustum cullingFrustum;
    @Shadow @Final private Minecraft minecraft;
    @Shadow private @Nullable ClientLevel level;
    @Shadow public abstract boolean shouldShowEntityOutlines();
    @Shadow @Final private BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    @Shadow @Final private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;
    @Shadow @Final private Set<BlockEntity> globalBlockEntities;

    @Unique
    private boolean outlineEffectRequested = false;

    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=sky"))
    private void nytheriacompat$earlySetupFog(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci, @Local(ordinal = 2) boolean isFoggy, @Local(ordinal = 1) float renderDistance, @Local(ordinal = 0) float partialTick) {
        // TODO: Is this actually needed?
        FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, renderDistance, isFoggy, partialTick);
    }

    @Definition(id = "camera", local = @Local(type = Camera.class, argsOnly = true))
    @Definition(id = "getEntity", method = "Lnet/minecraft/client/Camera;getEntity()Lnet/minecraft/world/entity/Entity;")
    @Expression("camera.getEntity() == ?")
    @ModifyExpressionValue(method = "renderLevel", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$renderLocalPlayerWhenNotCamera(boolean original, @Local Entity entity) {
        return original || (entity == minecraft.player && !minecraft.player.isSpectator());
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0))
    private <E> Iterator<E> nytheriacompat$removeInvisibleBlockEntities(Iterator<E> original, @Local Frustum frustum) {
        var renderDispatcher = blockEntityRenderDispatcher;
        return (Iterator<E>) new IteratorWrapper<BlockEntity>((Iterator<? extends BlockEntity>) original, (blockEntity) ->
            ClientHooks.isBlockEntityRendererVisible(renderDispatcher, blockEntity, frustum) ? blockEntity : null
        );
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"))
    private <E> Iterator<E> nytheriacompat$removeInvisibleGlobalBlockEntities(Iterator<E> original, @Local Frustum frustum) {
        var renderDispatcher = blockEntityRenderDispatcher;
        return (Iterator<E>) new IteratorWrapper<BlockEntity>((Iterator<? extends BlockEntity>) original, (blockEntity) ->
            ClientHooks.isBlockEntityRendererVisible(renderDispatcher, blockEntity, frustum) ? blockEntity : null
        );
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
    private void nytheriacompat$storeFrustumAndDispatchAfterParticles(ParticleEngine instance, LightTexture lightTexture, Camera camera, float f, Operation<Void> original, @Local Frustum frustum, @Local PoseStack poseStack, @Local(argsOnly = true, ordinal = 0) Matrix4f frustumMatrix, @Local(argsOnly = true, ordinal = 1) Matrix4f projectionMatrix) {
        instance.nytheriacompat$setClippingHelper(frustum);
        original.call(instance, lightTexture, camera, f);
        instance.nytheriacompat$setClippingHelper(null);

        ClientHooks.dispatchRenderStage(RenderLevelStageEvent.Stage.AFTER_PARTICLES, (LevelRenderer) (Object) this, poseStack, frustumMatrix, projectionMatrix, this.ticks, camera, frustum);
    }

    // Nytheria Compat: we're not doing Neo's particle rendering patch.

    // Nytheria Compat: we don't need to handle off-thread terrain setup

    // methods added in by Neo.
    // prefixed with nytheriacompat$i$ to "soft-implement" in the event another mod is using this same method signature,
    // and using @Intrinsic(displace = true) to try to ensure that our behaviour would still be called.
    // @Intrinsic alone would be used to try to prioritize the other mod's behaviour, typically for instances
    // where either a value is returned or if it's possible for things to potentially run twice when undesired.
    @Intrinsic
    public Frustum nytheriacompat$i$getFrustum() {
        return this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
    }

    @Intrinsic
    public int nytheriacompat$i$getTicks() {
        return ticks;
    }

    @Intrinsic
    public void nytheriacompat$i$iterateVisibleBlockEntities(Consumer<BlockEntity> blockEntityConsumer) {
        for (SectionRenderDispatcher.RenderSection chunkInfo : this.visibleSections) {
            chunkInfo.getCompiled().getRenderableBlockEntities().forEach(blockEntityConsumer);
        }

        synchronized (this.globalBlockEntities) {
            this.globalBlockEntities.forEach(blockEntityConsumer);
        }
    }

    @Intrinsic(displace = true)
    public void nytheriacompat$i$requestOutlineEffect() {
        this.outlineEffectRequested = true;
    }
}
