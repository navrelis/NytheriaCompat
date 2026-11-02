/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.client.particle.ParticleInjection;

@Mixin(Particle.class)
public abstract class ParticleInject implements ParticleInjection {
    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;

    @Shadow
    public abstract AABB getBoundingBox();

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return this.getBoundingBox().inflate(1.0);
    }

    @Override
    public Vec3 getPos() {
        return new Vec3(this.x, this.y, this.z);
    }
}
