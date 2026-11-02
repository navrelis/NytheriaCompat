/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on NeoForge by NeoForged, as ported by KiltMC (see VENDORED.md). Licensed under the GNU LGPL 2.1.
 */

package net.neoforged.neoforge.client.extensions;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;

/**
 * Extension interface for {@link Minecraft}.
 */
public interface IMinecraftExtension {
    private Minecraft self() {
        return (Minecraft) this;
    }

    /**
     * Pushes a screen as a new GUI layer.
     *
     * @param screen the new GUI layer
     */
    default void pushGuiLayer(Screen screen) {
        ClientHooks.pushGuiLayer(self(), screen);
    }

    /**
     * Pops a GUI layer from the screen.
     */
    default void popGuiLayer() {
        ClientHooks.popGuiLayer(self());
    }

    // Nytheria Compat: make sure to wrap the original call.
    default void nytheriacompat$popGuiLayer(Runnable onPop) {
        ClientHooks.nytheriacompat$popGuiLayer(self(), onPop);
    }

    /**
     * Retrieves the {@link Locale} set by the player.
     * Useful for creating string and number formatters.
     */
    default Locale getLocale() {
        return self().getLanguageManager().getJavaLocale();
    }
}
