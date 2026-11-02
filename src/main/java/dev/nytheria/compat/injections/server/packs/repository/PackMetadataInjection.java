/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.server.packs.repository;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.world.flag.FeatureFlagSet;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.List;

public interface PackMetadataInjection {
    static Pack.Metadata create(Component description, PackCompatibility compatibility, FeatureFlagSet requestedFeatures, List<String> overlays, boolean hidden) {
        var metadata = new Pack.Metadata(description, compatibility, requestedFeatures, overlays);
        metadata.nytheriacompat$markForge();
        metadata.nytheriacompat$setHidden(hidden);
        return metadata;
    }

    default void nytheriacompat$setHidden(boolean hidden) {
        throw NytheriaCompatHelper.createMixinException(PackMetadataInjection.class, "nytheriacompat$setHidden");
    }

    default void nytheriacompat$markForge() {
        throw NytheriaCompatHelper.createMixinException(PackMetadataInjection.class, "nytheriacompat$markForge");
    }

    default boolean hidden() {
        throw NytheriaCompatHelper.createMixinException(PackMetadataInjection.class, "hidden");
    }
}
