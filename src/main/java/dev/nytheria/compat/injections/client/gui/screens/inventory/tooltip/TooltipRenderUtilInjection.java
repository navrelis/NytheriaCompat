/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.GuiGraphics;
import dev.nytheria.compat.mixin.TooltipRenderUtilAccessor;

public interface TooltipRenderUtilInjection {
    static void renderTooltipBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int z, int backgroundTop, int backgroundBottom, int borderTop, int borderBottom) {
        int i = x - 3;
        int j = y - 3;
        int k = width + 3 + 3;
        int l = height + 3 + 3;

        TooltipRenderUtilAccessor.callRenderHorizontalLine(guiGraphics, i, j - 1, k, z, backgroundTop);
        TooltipRenderUtilAccessor.callRenderHorizontalLine(guiGraphics, i, j + l, k, z, backgroundBottom);
        renderRectangle(guiGraphics, i, j, k, l, z, backgroundTop, backgroundBottom);
        TooltipRenderUtilAccessor.callRenderVerticalLineGradient(guiGraphics, i - 1, j, l, z, backgroundTop, backgroundBottom);
        TooltipRenderUtilAccessor.callRenderVerticalLineGradient(guiGraphics, i + k, j, l, z, backgroundTop, backgroundBottom);
        TooltipRenderUtilAccessor.callRenderFrameGradient(guiGraphics, i, j + 1, k, l, z, borderTop, borderBottom);
    }

    private static void renderRectangle(GuiGraphics guiGraphics, int x, int y, int width, int height, int z, int colorFrom, int colorTo) {
        guiGraphics.fillGradient(x, y, x + width, y + height, colorFrom, colorTo);
    }
}
