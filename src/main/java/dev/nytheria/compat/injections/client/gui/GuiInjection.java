/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.gui;

import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.client.gui.GuiGraphics;

public interface GuiInjection {
    default void renderSelectedItemName(GuiGraphics guiGraphics, int yShift) {
        throw NytheriaCompatHelper.createMixinException(GuiInjection.class, "renderSelectedItemName");
    }

    default void initModdedOverlays() {
        throw NytheriaCompatHelper.createMixinException(GuiInjection.class, "initModdedOverlays");
    }

    default int getLayerCount() {
        throw NytheriaCompatHelper.createMixinException(GuiInjection.class, "getLayerCount");
    }
}
