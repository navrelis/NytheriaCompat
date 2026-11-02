// TRACKED HASH: 66858683859d22896e8fbc2be116d575c4c260c3
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui;

import java.util.List;
import java.util.Optional;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.extensions.IGuiGraphicsExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.client.gui.GuiGraphicsInjection;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsInject implements GuiGraphicsInjection, IGuiGraphicsExtension {

    @Shadow public abstract void renderTooltip(Font font, List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent, int mouseX, int mouseY);
    @Shadow public abstract int guiWidth();
    @Shadow public abstract int guiHeight();

    @Unique private ItemStack tooltipStack = ItemStack.EMPTY;

    @Override
    public void nytheriacompat$setTooltipStack(ItemStack stack) {
        this.tooltipStack = stack;
    }

    @Override
    public void renderTooltip(Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY) {
        this.tooltipStack = stack;
        this.renderTooltip(font, textComponents, tooltipComponent, mouseX, mouseY);
        this.tooltipStack = ItemStack.EMPTY;
    }

    // Nytheria Compat: We're using fields rather than @Share here, because we enter a separate method (lambda) within renderTooltipInternal.
    @Unique private RenderTooltipEvent.Pre nytheriacompat$preEvent;
    @Unique private List<ClientTooltipComponent> nytheriacompat$components;

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"))
    private void nytheriacompat$storeComponents(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY, ClientTooltipPositioner tooltipPositioner, CallbackInfo ci) {
        this.nytheriacompat$components = components;
    }

    @Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void nytheriacompat$callPreTooltipRenderEvent(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY, ClientTooltipPositioner tooltipPositioner, CallbackInfo ci, @Share(value = "preEvent", namespace = "nytheria_compat") LocalRef<RenderTooltipEvent.Pre> preEvent) {
        nytheriacompat$preEvent = ClientHooks.onRenderTooltipPre(this.tooltipStack, (GuiGraphics) (Object) this, mouseX, mouseY, guiWidth(), guiHeight(), components, font, tooltipPositioner);
        preEvent.set(nytheriacompat$preEvent); // Nytheria Compat: For mixin modifiers

        if (nytheriacompat$preEvent.isCanceled())
            ci.cancel();
    }

}