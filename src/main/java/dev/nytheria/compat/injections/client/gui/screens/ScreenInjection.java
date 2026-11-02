/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface ScreenInjection {
    default void nytheriacompat$addEventWidget(GuiEventListener b) {
        throw new IllegalStateException();
    }
    default Minecraft getMinecraft() {
        throw NytheriaCompatHelper.createMixinException(ScreenInjection.class, "getMinecraft");
    }
}
