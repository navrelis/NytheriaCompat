/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.server.packs.repository;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.List;
import java.util.stream.Stream;

public interface PackInjection {
    PackSource CHILD_SOURCE = PackSource.create(
            name -> Component.translatable(
                    "pack.nameAndSource",
                    name,
                    Component.translatable("pack.neoforge.source.child")
            ).withStyle(net.minecraft.ChatFormatting.GRAY),
            false
    ); // Neo: Pack source for child packs; should not be otherwise used

    static Pack create(PackLocationInfo location, Pack.ResourcesSupplier resources, Pack.Metadata metadata, PackSelectionConfig selectionConfig, List<Pack> children) {
        var pack = new Pack(location, resources, metadata, selectionConfig);
        List<Pack> flattenedChildren = new java.util.ArrayList<>();
        List<Pack> remainingChildren = children;
        // recursively flatten children
        while (!remainingChildren.isEmpty()) {
            List<Pack> oldChildren = remainingChildren;
            remainingChildren = new java.util.ArrayList<>();
            for (Pack child : oldChildren) {
                // Adapts the child pack with the following changes:
                // - Must be hidden
                // - Must have no children
                // - Has a pack source of CHILD_SOURCE, which is not automatically added
                Pack adaptedChild = new Pack(
                        new PackLocationInfo(child.location().id(), child.location().title(), CHILD_SOURCE, child.location().knownPackInfo()),
                        child.resources,
                        PackMetadataInjection.create(child.metadata.description(), child.metadata.compatibility(), child.metadata.requestedFeatures(), child.metadata.overlays(), true),
                        new PackSelectionConfig(false, child.getDefaultPosition(), child.isFixedPosition())
                );
                adaptedChild.nytheriacompat$setChildren(List.of());
                flattenedChildren.add(adaptedChild);
                remainingChildren.addAll(child.getChildren());
            }
        }

        pack.nytheriacompat$setChildren(List.copyOf(flattenedChildren));
        return pack;
    }

    default Pack hidden() {
        throw NytheriaCompatHelper.createMixinException(PackInjection.class, "hidden");
    }

    default boolean isHidden() {
        throw NytheriaCompatHelper.createMixinException(PackInjection.class, "isHidden");
    }

    default List<Pack> getChildren() {
        throw NytheriaCompatHelper.createMixinException(PackInjection.class, "getChildren");
    }

    default Stream<Pack> streamSelfAndChildren() {
        throw NytheriaCompatHelper.createMixinException(PackInjection.class, "streamSelfAndChildren");
    }

    default Pack withChildren(List<Pack> children) {
        throw NytheriaCompatHelper.createMixinException(PackInjection.class, "withChildren");
    }

    default void nytheriacompat$setChildren(List<Pack> children) {
        throw NytheriaCompatHelper.createMixinException(PackInjection.class, "nytheriacompat$setChildren");
    }
}
