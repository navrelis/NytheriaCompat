// TRACKED HASH: 0abe6b86598c641126d968f40fad8310d1fff167
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui.screens;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.client.gui.screens.ScreenInjection;
import dev.nytheria.compat.mixin.ScreenAccessor;

import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenInject implements ScreenInjection {
    @Shadow public int height;
    @Shadow public int width;
    @Shadow @Nullable protected Minecraft minecraft;
    @Shadow @Final private List<GuiEventListener> children;
    @Shadow public List<Renderable> renderables;
    @Shadow @Final private List<NarratableEntry> narratables;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    private void nytheriacompat$renderContainerScreenBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // Nytheria Compat: we're not fucking replicating the super method. we just move it here instead.
        if ((Object) this instanceof AbstractContainerScreen<?>) {
            if (EventGate.has(ContainerScreenEvent.Render.Background.class)) NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Background((AbstractContainerScreen<?>) (Object) this, guiGraphics, mouseX, mouseY));
        }
    }

    @WrapOperation(method = {"init(Lnet/minecraft/client/Minecraft;II)V", "rebuildWidgets"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init()V"))
    private void nytheriacompat$tryCallForgeScreenInit(Screen instance, Operation<Void> original) {
        if (!(EventGate.has(ScreenEvent.Init.Pre.class) && NeoForge.EVENT_BUS.post(new ScreenEvent.Init.Pre((Screen) (Object) this, this.children, ((ScreenInjection) instance)::nytheriacompat$addEventWidget, ((ScreenAccessor) this)::callRemoveWidget)).isCanceled())) {
            original.call(instance);
        }

        if (EventGate.has(ScreenEvent.Init.Post.class)) NeoForge.EVENT_BUS.post(new ScreenEvent.Init.Post((Screen) (Object) this, this.children, ((ScreenInjection) instance)::nytheriacompat$addEventWidget, ((ScreenAccessor) this)::callRemoveWidget));
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"))
    private void nytheriacompat$fixNeoIssue1504(float partialTick, CallbackInfo ci) {
        RenderSystem.disableDepthTest();
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    private void addEventWidget(GuiEventListener b) {
        if (b instanceof Renderable r)
            this.renderables.add(r);

        if (b instanceof NarratableEntry ne)
            this.narratables.add(ne);

        this.children.add(b);
    }

    // We don't want to make the above method public, so.
    @Override
    public void nytheriacompat$addEventWidget(GuiEventListener b) {
        this.addEventWidget(b);
    }
}