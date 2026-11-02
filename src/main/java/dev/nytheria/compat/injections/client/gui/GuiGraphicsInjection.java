/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.List;
import java.util.Optional;

public interface GuiGraphicsInjection {
    default int drawString(Font font, FormattedCharSequence text, float x, float y, int color, boolean dropShadow) {
        throw NytheriaCompatHelper.createMixinException(GuiGraphicsInjection.class, "drawString");
    }

    default int drawString(Font font, @Nullable String text, float x, float y, int color, boolean dropShadow)  {
        throw NytheriaCompatHelper.createMixinException(GuiGraphicsInjection.class, "drawString");
    }

    default void blitRepeating(ResourceLocation atlasLocation, int x, int y, int width, int height, int uOffset, int vOffset, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        throw NytheriaCompatHelper.createMixinException(GuiGraphicsInjection.class, "blitRepeating");
    }

    default void renderTooltip(Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY) {
        throw NytheriaCompatHelper.createMixinException(GuiGraphicsInjection.class, "renderTooltip");
    }

    default void nytheriacompat$setTooltipStack(ItemStack stack) {
        throw NytheriaCompatHelper.createMixinException(GuiGraphicsInjection.class, "nytheriacompat$setTooltipStack");
    }
}
