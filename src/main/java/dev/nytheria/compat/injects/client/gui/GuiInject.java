// TRACKED HASH: fd1859323d2b7b647915a5c458b0159a1f4e13b1
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui;

import java.util.Iterator;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.client.gui.GuiInjection;
import dev.nytheria.compat.util.IteratorWrapper;
import dev.nytheria.compat.workarounds.NytheriaCompatGuiLayers;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.world.effect.MobEffectInstance;

@Mixin(Gui.class)
public abstract class GuiInject implements GuiInjection {
    @Shadow @Final private LayeredDraw layers;
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ChatComponent chat;
    @Shadow protected abstract void renderSelectedItemName(GuiGraphics guiGraphics);

    @Unique private GuiLayerManager layerManager;

    @Unique public int leftHeight;
    @Unique public int rightHeight;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;<init>(Lnet/minecraft/client/Minecraft;)V"))
    private void nytheriacompat$storeLayerManager(Minecraft minecraft, CallbackInfo ci) {
        this.layerManager = this.layers.nytheriacompat$getLayerManager();
    }

    @Definition(id = "add", method = "Lnet/minecraft/client/gui/LayeredDraw;add(Lnet/minecraft/client/gui/LayeredDraw$Layer;)Lnet/minecraft/client/gui/LayeredDraw;")
    @Definition(id = "renderHotbarAndDecorations", method = "Lnet/minecraft/client/gui/Gui;renderHotbarAndDecorations(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V")
    @Expression("?.add(this::renderHotbarAndDecorations)")
    @WrapOperation(method = "<init>", at = @At("MIXINEXTRAS:EXPRESSION"))
    private LayeredDraw nytheriacompat$namedHotbarAndDecorations(LayeredDraw instance, LayeredDraw.Layer layer, Operation<LayeredDraw> original) {
        instance.nytheriacompat$addVanilla(NytheriaCompatGuiLayers.HOTBAR_AND_DECORATIONS, layer);

        instance.nytheriacompat$add(VanillaGuiLayers.HOTBAR);
        instance.nytheriacompat$add(VanillaGuiLayers.JUMP_METER);
        instance.nytheriacompat$add(VanillaGuiLayers.EXPERIENCE_BAR);

        var playerHealthComponents = new LayeredDraw();
        playerHealthComponents.nytheriacompat$add(VanillaGuiLayers.PLAYER_HEALTH);
        playerHealthComponents.nytheriacompat$add(VanillaGuiLayers.ARMOR_LEVEL);
        playerHealthComponents.nytheriacompat$add(VanillaGuiLayers.FOOD_LEVEL);

        instance.add(playerHealthComponents, () -> this.minecraft.gameMode.canHurtPlayer())
            .nytheriacompat$add(VanillaGuiLayers.VEHICLE_HEALTH);
        instance.nytheriacompat$add(VanillaGuiLayers.AIR_LEVEL);
        instance.nytheriacompat$add(VanillaGuiLayers.SELECTED_ITEM_NAME);
        instance.nytheriacompat$add(VanillaGuiLayers.SPECTATOR_TOOLTIP);

        return original.call(instance, layer);
    }

    @WrapMethod(method = "render")
    private void nytheriacompat$callGuiRenderEvents(GuiGraphics guiGraphics, DeltaTracker deltaTracker, Operation<Void> original) {
        this.leftHeight = 39;
        this.rightHeight = 39;

        if (EventGate.has(RenderGuiEvent.Pre.class) && NeoForge.EVENT_BUS.post(new RenderGuiEvent.Pre(guiGraphics, deltaTracker)).isCanceled()) {
            return;
        }

        original.call(guiGraphics, deltaTracker);

        if (EventGate.has(RenderGuiEvent.Post.class)) NeoForge.EVENT_BUS.post(new RenderGuiEvent.Post(guiGraphics, deltaTracker));
    }

    @ModifyExpressionValue(method = "renderEffects", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0))
    private <E extends MobEffectInstance> Iterator<E> nytheriacompat$wrapWithVisibleEffectCheck(Iterator<E> original) {
        return new IteratorWrapper<>(original, effect -> {
            var renderer = IClientMobEffectExtensions.of(effect);
            if (!renderer.isVisibleInGui(effect))
                return null;

            return effect;
        });
    }

    @Unique private int nytheriacompat$yShift = Integer.MIN_VALUE;

    @Override
    public void renderSelectedItemName(GuiGraphics graphics, int yShift) {
        this.nytheriacompat$yShift = yShift;
        this.renderSelectedItemName(graphics);
        this.nytheriacompat$yShift = 0;
    }

    @Definition(id = "r", local = @Local(type = int.class, ordinal = 8))
    @Definition(id = "n", local = @Local(type = int.class, ordinal = 4))
    @Expression("r = n - 10")
    @Inject(method = "renderPlayerHealth", at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER))
    private void nytheriacompat$shiftLeftHeight(GuiGraphics guiGraphics, CallbackInfo ci, @Local(ordinal = 6) int p, @Local(ordinal = 7) int q) {
        this.leftHeight += (p - 1) * q + 10;
    }

    @Inject(method = "renderPlayerHealth", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableBlend()V", shift = At.Shift.AFTER))
    private void nytheriacompat$incrementRightHeightAfterAir(GuiGraphics guiGraphics, CallbackInfo ci) {
        this.rightHeight += 10;
    }

    @Inject(method = "renderVehicleHealth", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I")) // Nytheria Compat: this isn't the right injection point, but close enough.
    private void nytheriacompat$incrementRightHeight(GuiGraphics guiGraphics, CallbackInfo ci) {
        this.rightHeight += 10;
    }

    @ApiStatus.Internal
    public void initModdedOverlays() {
        this.layerManager.initModdedLayers();
        this.layers.nytheriacompat$updateInternalLayers();
    }

    @Override
    public int getLayerCount() {
        return Math.max(this.layerManager.getLayerCount(), this.layers.nytheriacompat$getLayerCount());
    }

    @Mixin(Gui.HeartType.class)
    public abstract static class HeartTypeInject implements IExtensibleEnum {
        @CreateStatic
        private static ExtensionInfo getExtensionInfo() {
            return ExtensionInfo.nonExtended(Gui.HeartType.class);
        }
    }
}
