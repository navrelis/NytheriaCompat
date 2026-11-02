/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.flag;

import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.world.flag.FeatureFlagUniverse;

public interface FeatureFlagInjection {
    default int nytheriacompat$extMaskIndex() {
        throw NytheriaCompatHelper.createMixinException(FeatureFlagInjection.class, "nytheriacompat$extMaskIndex");
    }

    default void nytheriacompat$setExtMaskIndex(int index) {
        throw NytheriaCompatHelper.createMixinException(FeatureFlagInjection.class, "nytheriacompat$setExtMaskIndex");
    }

    default boolean isModded() {
        throw NytheriaCompatHelper.createMixinException(FeatureFlagInjection.class, "isModded");
    }

    default void nytheriacompat$setModded(boolean modded) {
        throw NytheriaCompatHelper.createMixinException(FeatureFlagInjection.class, "nytheriacompat$setModded");
    }

    default FeatureFlagUniverse nytheriacompat$universe() {
        throw NytheriaCompatHelper.createMixinException(FeatureFlagInjection.class, "nytheriacompat$universe");
    }

    default long nytheriacompat$mask() {
        throw NytheriaCompatHelper.createMixinException(FeatureFlagInjection.class, "nytheriacompat$mask");
    }
}
