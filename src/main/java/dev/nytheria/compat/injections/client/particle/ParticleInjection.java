/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.particle;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface ParticleInjection {
    default AABB getRenderBoundingBox(float partialTicks) {
        throw NytheriaCompatHelper.createMixinException(ParticleInjection.class, "getRenderBoundingBox");
    }

    default Vec3 getPos() {
        throw NytheriaCompatHelper.createMixinException(ParticleInjection.class, "getPos");
    }
}
