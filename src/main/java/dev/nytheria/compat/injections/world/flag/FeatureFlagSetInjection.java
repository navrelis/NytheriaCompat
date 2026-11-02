/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.flag;

import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface FeatureFlagSetInjection {
    default long[] nytheriacompat$extendedMask() {
        throw NytheriaCompatHelper.createMixinException(FeatureFlagSetInjection.class, "nytheriacompat$extendedMask");
    }

    default void nytheriacompat$setExtendedMask(long[] extendedMask) {
        throw NytheriaCompatHelper.createMixinException(FeatureFlagSetInjection.class, "nytheriacompat$setExtendedMask");
    }
}
