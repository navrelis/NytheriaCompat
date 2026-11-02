/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.phys;

import net.minecraft.world.phys.AABB;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface AABBInjection {
    AABB INFINITE = new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    default boolean isInfinite() {
        throw NytheriaCompatHelper.createMixinException(AABBInjection.class, "isInfinite");
    }
}
