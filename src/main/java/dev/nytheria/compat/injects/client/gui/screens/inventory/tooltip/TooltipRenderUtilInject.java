// TRACKED HASH: 8163397c1ab77801d2732a96d6b5fb5aeec201ba
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.client.gui.screens.inventory.tooltip.TooltipRenderUtilInjection;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilInject implements TooltipRenderUtilInjection {
    @CreateStatic
    private static void renderTooltipBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int z, int backgroundTop, int backgroundBottom, int borderTop, int borderBottom) {
        TooltipRenderUtilInjection.renderTooltipBackground(guiGraphics, x, y, width, height, z, backgroundTop, backgroundBottom, borderTop, borderBottom);
    }
}