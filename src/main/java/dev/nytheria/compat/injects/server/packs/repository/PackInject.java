// TRACKED HASH: 3c15b31dfec44b9332f86e2c6d5babfb7b181f59
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.packs.repository;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.world.flag.FeatureFlagSet;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.server.packs.OverlayMetadataSectionInjection;
import dev.nytheria.compat.injections.server.packs.repository.PackMetadataInjection;
import dev.nytheria.compat.injections.server.packs.repository.PackInjection;
import dev.nytheria.compat.util.NeoForgeSources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Mixin(Pack.class)
public abstract class PackInject implements PackInjection {
    @Shadow
    @Final
    private PackSelectionConfig selectionConfig;
    @Shadow
    @Final
    private Pack.Metadata metadata;
    @Shadow
    @Final
    private Pack.ResourcesSupplier resources;
    @Shadow
    @Final
    private PackLocationInfo location;

    @Shadow @Final private static Logger LOGGER;

    @Unique private boolean hidden;
    private List<Pack> children;

    public PackInject(PackLocationInfo location, Pack.ResourcesSupplier resources, Pack.Metadata metadata, PackSelectionConfig selectionConfig) {}

    @CreateInitializer
    private PackInject(PackLocationInfo location, Pack.ResourcesSupplier resources, Pack.Metadata metadata, PackSelectionConfig selectionConfig, List<Pack> children) {
        this(location, resources, metadata, selectionConfig);

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
                Pack adaptedChild = PackInjection.create(
                        new PackLocationInfo(child.location().id(), child.location().title(), CHILD_SOURCE, child.location().knownPackInfo()),
                        child.resources,
                        PackMetadataInjection.create(child.metadata.description(), child.metadata.compatibility(), child.metadata.requestedFeatures(), child.metadata.overlays(), true),
                        new PackSelectionConfig(false, child.getDefaultPosition(), child.isFixedPosition()),
                        List.of()
                );
                flattenedChildren.add(adaptedChild);
                remainingChildren.addAll(child.getChildren());
            }
        }
        this.children = List.copyOf(flattenedChildren);
    }

    @WrapOperation(method = "readPackMetadata", at = @At(value = "NEW", target = "(Lnet/minecraft/network/chat/Component;Lnet/minecraft/server/packs/repository/PackCompatibility;Lnet/minecraft/world/flag/FeatureFlagSet;Ljava/util/List;)Lnet/minecraft/server/packs/repository/Pack$Metadata;"))
    private static Pack.Metadata nytheriacompat$addForgeDataToInfo(Component component, PackCompatibility packCompatibility, FeatureFlagSet featureFlagSet, List<String> list, Operation<Pack.Metadata> original, @Local(argsOnly = true) int version, @Local PackMetadataSection section, @Local PackResources resources) throws IOException {
        var neoOverlays = resources.getMetadataSection(OverlayMetadataSectionInjection.NEOFORGE_TYPE);
        if (neoOverlays != null) {
            // Nytheria Compat: neoforge:overlays only apply to NeoForge mods' packs (plain Fabric ignores them everywhere else), and a
            // pack that also has fabric:overlays gets its overlays from Fabric API with the author's Fabric conditions
            boolean neoForgeSource = NeoForgeSources.appliesNeoForgeData(resources.packId());
            boolean declaresFabricOverlays = resources.getMetadataSection(OverlayMetadataSectionInjection.FABRIC_TYPE) != null;
            List<String> neoForgeOverlays = neoOverlays.overlaysForVersion(version);
            if (!neoForgeSource && !neoForgeOverlays.isEmpty()) {
                NeoForgeSources.logIgnoredOverlays(resources.packId(), neoForgeOverlays);
            } else if (declaresFabricOverlays && !neoForgeOverlays.isEmpty()) {
                LOGGER.debug("Pack {} declares fabric:overlays, leaving its neoforge:overlays {} to Fabric API", resources.packId(), neoForgeOverlays);
            }
            list = new ArrayList<>(list);
            list.addAll(OverlayMetadataSectionInjection.neoForgeOverlaysToAdd(neoForgeSource, declaresFabricOverlays, neoForgeOverlays));
            list = List.copyOf(list);
        }
        var info = original.call(component, packCompatibility, featureFlagSet, list);
        info.nytheriacompat$markForge();
        info.nytheriacompat$setHidden(resources.isHidden());

        return info;
    }

    @Override
    public Pack hidden() {
        return PackInjection.create(
            new PackLocationInfo(this.location.id(), this.location.title(), this.location.source(), this.location.knownPackInfo()),
            this.resources,
            PackMetadataInjection.create(this.metadata.description(), this.metadata.compatibility(), this.metadata.requestedFeatures(), this.metadata.overlays(), true),
            new PackSelectionConfig(this.selectionConfig.required(), this.selectionConfig.defaultPosition(), this.selectionConfig.fixedPosition()),
            this.children
        );
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public List<Pack> getChildren() {
        return children;
    }

    @Override
    public void nytheriacompat$setChildren(List<Pack> children) {
        this.children = children;
    }

    @Override
    public Stream<Pack> streamSelfAndChildren() {
        return Stream.concat(Stream.of((Pack) (Object) this), children.stream());
    }

    @Override
    public Pack withChildren(List<Pack> children) {
        return PackInjection.create(this.location, this.resources, this.metadata, this.selectionConfig, children);
    }

    @Mixin(Pack.Metadata.class)
    public abstract static class MetadataInject implements PackMetadataInjection {
        @Unique private boolean nytheriacompat$isForge = false;
        @Unique private boolean hidden;

        public MetadataInject(Component description, PackCompatibility compatibility, FeatureFlagSet requestedFeatures, List<String> overlays) {}

        @CreateInitializer
        public MetadataInject(Component description, PackCompatibility compatibility, FeatureFlagSet requestedFeatures, List<String> overlays, boolean hidden) {
            this(description, compatibility, requestedFeatures, overlays);
            this.hidden = hidden;
            this.nytheriacompat$isForge = true;
        }

        @Override
        public void nytheriacompat$markForge() {
            this.nytheriacompat$isForge = true;
        }

        @Override
        public boolean hidden() {
            return this.hidden;
        }

        @Override
        public void nytheriacompat$setHidden(boolean hidden) {
            this.hidden = hidden;
        }
    }
}