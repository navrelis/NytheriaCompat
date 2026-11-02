// TRACKED HASH: c7da1c1b82a8d0a88d3f2a674ea41109be948d6c
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui.screens.inventory;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.client.gui.screens.inventory.AbstractContainerScreenInjection;
import dev.nytheria.compat.mixin.client.gui.screens.inventory.AbstractContainerScreenAccessor;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenInject extends Screen implements AbstractContainerScreenInjection {
    protected AbstractContainerScreenInject(Component component) {
        super(component);
    }

    @Shadow @Nullable protected Slot hoveredSlot;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;

    @CreateStatic
    private static void renderSlotHighlight(GuiGraphics guiGraphics, int x, int y, int blitOffset, int slotColor) {
        AbstractContainerScreenInjection.nytheriacompat$slotColor.set(slotColor);
        // this is called: workarounds.
        AbstractContainerScreenAccessor.invokeRenderSlotHighlight(guiGraphics, x, y, blitOffset);
        AbstractContainerScreenInjection.nytheriacompat$slotColor.set(defaultSlotColor);
    }

    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (slot.isHighlightable()) {
            renderSlotHighlight(guiGraphics, slot.x, slot.y, 0, this.getSlotColor(slot.index));
        }
    }

    @Inject(method = "renderFloatingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    public void nytheriacompat$getItemFont(GuiGraphics guiGraphics, ItemStack stack, int x, int y, String text, CallbackInfo ci, @Share("font") LocalRef<Font> font) {
        font.set(IClientItemExtensions.of(stack).getFont(stack, IClientItemExtensions.FontContext.ITEM_COUNT));

        if (font.get() == null)
            font.set(this.font);
    }

    protected void renderSlotContents(GuiGraphics guiGraphics, ItemStack stack, Slot slot, @Nullable String countString) {
        int k = slot.x + slot.y * this.imageWidth;
        if (slot.isFake()) {
            guiGraphics.renderFakeItem(stack, slot.x, slot.y, k);
        } else {
            guiGraphics.renderItem(stack, slot.x, slot.y, k);
        }

        guiGraphics.renderItemDecorations(this.font, stack, slot.x, slot.y, countString);
    }

    // Nytheria Compat TODO: how do we implement the isActiveAndMatches patch properly

    @ModifyVariable(method = "mouseClicked", at = @At("STORE"), ordinal = 1)
    public boolean nytheriacompat$ensureNoSlotInEmptySpaceWhenClicked(boolean hasClicked, @Local Slot slot) {
        if (slot != null)
            return false;
        else
            return hasClicked;
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    public void nytheriacompat$callParentRelease(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        super.mouseReleased(mouseX, mouseY, button);
    }

    @ModifyVariable(method = "mouseReleased", at = @At("STORE"))
    public boolean nytheriacompat$ensureNoSlotInEmptySpaceWhenReleased(boolean hasClicked, @Local Slot slot) {
        if (slot != null)
            return false;
        else
            return hasClicked;
    }

    @Definition(id = "slot2", local = @Local(type = Slot.class, ordinal = 1))
    @Definition(id = "container", field = "Lnet/minecraft/world/inventory/Slot;container:Lnet/minecraft/world/Container;")
    @Definition(id = "slot", local = @Local(type = Slot.class, ordinal = 0))
    @Expression("slot2.container == slot.container")
    @ModifyExpressionValue(method = "mouseReleased", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$useForgeInventoryCheck(boolean original, @Local(ordinal = 0) Slot slot, @Local(ordinal = 1) Slot slot2) {
        return original || slot.isSameInventory(slot2);
    }

    // Nytheria Compat TODO: do we implement MC-146650?

    @Nullable
    public Slot getSlotUnderMouse() {
        return this.hoveredSlot;
    }

    public int getGuiLeft() {
        return this.leftPos;
    }

    public int getGuiTop() {
        return this.topPos;
    }

    protected int slotColor = -2130706433;
    public int getSlotColor(int index) {
        return slotColor;
    }

    @Override
    public void nytheriacompat$setSlotColor(int slotColor) {
        this.slotColor = slotColor;
    }
}