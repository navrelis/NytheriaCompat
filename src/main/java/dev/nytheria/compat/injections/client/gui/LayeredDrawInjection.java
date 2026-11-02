/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.gui;

import java.util.Collection;
import java.util.Map;

import net.neoforged.neoforge.client.gui.GuiLayerManager;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

public interface LayeredDrawInjection {
    default GuiLayerManager nytheriacompat$getLayerManager() {
        throw NytheriaCompatHelper.createMixinException(LayeredDrawInjection.class, "nytheriacompat$getLayerManager");
    }

    default Collection<ResourceLocation> nytheriacompat$getOrderedLayerIds() {
        throw NytheriaCompatHelper.createMixinException(LayeredDrawInjection.class, "nytheriacompat$getOrderedLayerIds");
    }

    default Map<ResourceLocation, LayeredDraw.Layer> nytheriacompat$getNamedLayers() {
        throw NytheriaCompatHelper.createMixinException(LayeredDrawInjection.class, "nytheriacompat$getNamedLayers");
    }

    default void nytheriacompat$add(ResourceLocation id) {
        this.nytheriacompat$addVanilla(id, GuiLayerManager.NYTHERIACOMPAT_EMPTY_LAYER);
    }

    default void nytheriacompat$addVanilla(ResourceLocation id, LayeredDraw.Layer layer) {
        throw NytheriaCompatHelper.createMixinException(LayeredDrawInjection.class, "nytheriacompat$addVanilla");
    }

    default Collection<LayeredDraw> nytheriacompat$getInnerDraws() {
        throw NytheriaCompatHelper.createMixinException(LayeredDrawInjection.class, "nytheriacompat$getInnerDraws");
    }

    default void nytheriacompat$updateInternalLayers() {
        throw NytheriaCompatHelper.createMixinException(LayeredDrawInjection.class, "nytheriacompat$updateInternalLayers");
    }

    default int nytheriacompat$getLayerCount() {
        throw NytheriaCompatHelper.createMixinException(LayeredDrawInjection.class, "nytheriacompat$getLayerCount");
    }
}
