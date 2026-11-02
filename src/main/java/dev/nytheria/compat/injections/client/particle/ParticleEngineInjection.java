/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.particle;

import java.util.function.Consumer;
import java.util.function.Predicate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

public interface ParticleEngineInjection {
    default void nytheriacompat$setClippingHelper(Frustum frustum) {}
    default void render(LightTexture lightTexture, Camera camera, float tickDelta, @Nullable Frustum clippingHelper, Predicate<ParticleRenderType> renderTypePredicate) {}
    default void addBlockHitEffects(BlockPos pos, BlockHitResult target) {}
    default void nytheriacompat$addBlockHitEffects(BlockPos pos, BlockHitResult target, Direction direction, Operation<Void> original) {}

    default void iterateParticles(Consumer<Particle> consumer) {
        throw NytheriaCompatHelper.createMixinException(ParticleEngineInjection.class, "iterateParticles");
    }

}
