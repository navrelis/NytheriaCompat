/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.flag;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlag;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.Map;

public interface FeatureFlagRegistryInjection {
    default Map<ResourceLocation, FeatureFlag> getAllFlags() {
        throw NytheriaCompatHelper.createMixinException(FeatureFlagInjection.class, "getAllFlags");
    }

    default boolean hasAnyModdedFlags() {
        throw NytheriaCompatHelper.createMixinException(FeatureFlagInjection.class, "hasAnyModdedFlags");
    }

    interface BuilderInjection {
        default FeatureFlag create(ResourceLocation id, boolean modded) {
            throw NytheriaCompatHelper.createMixinException(FeatureFlagRegistryInjection.BuilderInjection.class, "create");
        }
    }
}
