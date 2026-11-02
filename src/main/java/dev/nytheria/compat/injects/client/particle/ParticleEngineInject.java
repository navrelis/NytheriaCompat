/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.particle;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.NytheriaCompat;
import dev.nytheria.compat.injections.client.particle.ParticleEngineInjection;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineInject implements ParticleEngineInjection {
    @Shadow @Final @Mutable private Map<ParticleRenderType, Queue<Particle>> particles;
    @Shadow @Final private static List<ParticleRenderType> RENDER_ORDER;
    @Shadow protected ClientLevel level;
    @Shadow public abstract void crack(BlockPos pos, Direction side);

    @Shadow public abstract void render(LightTexture lightTexture, Camera camera, float f);

    @Unique private @Nullable Frustum nytheriacompat$clippingHelper;
    @Unique private Predicate<ParticleRenderType> nytheriacompat$renderTypePredicate;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nytheriacompat$handleParticleTypeComparator(ClientLevel level, TextureManager textureManager, CallbackInfo ci) {
        var oldMap = this.particles;
        this.particles = Maps.newTreeMap(ClientHooks.makeParticleRenderTypeComparator(RENDER_ORDER));

        this.particles.putAll(oldMap);
    }

    @Override
    public void nytheriacompat$setClippingHelper(Frustum frustum) {
        this.nytheriacompat$clippingHelper = frustum;
    }

    @Override
    public void render(LightTexture lightTexture, Camera camera, float tickDelta, @Nullable Frustum clippingHelper, Predicate<ParticleRenderType> renderTypePredicate) {
        this.nytheriacompat$clippingHelper = clippingHelper;
        this.nytheriacompat$renderTypePredicate = renderTypePredicate;
        this.render(lightTexture, camera, tickDelta);
        this.nytheriacompat$clippingHelper = null;
        this.nytheriacompat$renderTypePredicate = null;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void nytheriacompat$initShareData(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci,
                                    @Share(value = "frustum", namespace = NytheriaCompat.MOD_ID) LocalRef<Frustum> frustum,
                                    @Share(value = "renderTypePredicate", namespace = NytheriaCompat.MOD_ID) LocalRef<Predicate<ParticleRenderType>> renderTypePredicate) {
        frustum.set(this.nytheriacompat$clippingHelper);
        renderTypePredicate.set(Objects.requireNonNullElse(this.nytheriacompat$renderTypePredicate, $ -> true));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V", shift = At.Shift.AFTER, ordinal = 0, remap = false))
    private void nytheriacompat$initActiveTexture(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        RenderSystem.activeTexture(GL32C.GL_TEXTURE2);
        RenderSystem.activeTexture(GL32C.GL_TEXTURE0);
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/ParticleEngine;RENDER_ORDER:Ljava/util/List;"))
    private List<ParticleRenderType> nytheriacompat$mergeCustomParticles(List<ParticleRenderType> original, @Share(value = "renderTypePredicate", namespace = NytheriaCompat.MOD_ID) LocalRef<Predicate<ParticleRenderType>> renderTypePredicate) {
        var set = new LinkedHashSet<ParticleRenderType>();
        set.addAll(RENDER_ORDER);
        set.addAll(this.particles.keySet());

        var predicate = renderTypePredicate.get();
        if (predicate == null)
            predicate = $ -> true;

        set.removeIf(type -> type == ParticleRenderType.NO_RENDER);

        Predicate<ParticleRenderType> finalPredicate = predicate;
        set.removeIf(t -> !finalPredicate.test(t));

        return set.stream().toList();
    }

    @Override
    public void iterateParticles(Consumer<Particle> consumer) {
        for (ParticleRenderType particleRenderType : this.particles.keySet()) {
            if (particleRenderType == ParticleRenderType.NO_RENDER)
                continue;

            Iterable<Particle> particles = this.particles.get(particleRenderType);
            if (particles != null) {
                particles.forEach(consumer);
            }
        }
    }

    @Override
    public void addBlockHitEffects(BlockPos pos, BlockHitResult target) {
        nytheriacompat$addBlockHitEffects(pos, target, target.getDirection(), args -> {
            ((ParticleEngine) args[0]).crack((BlockPos) args[1], (Direction) args[2]);
            return null;
        });
    }

    @Override
    public void nytheriacompat$addBlockHitEffects(BlockPos pos, BlockHitResult target, Direction direction, Operation<Void> original) {
        var state = this.level.getBlockState(pos);

        if (!IClientBlockExtensions.of(state).addHitEffects(state, this.level, target, (ParticleEngine) (Object) this))
            original.call(this, pos, direction);
    }

}
