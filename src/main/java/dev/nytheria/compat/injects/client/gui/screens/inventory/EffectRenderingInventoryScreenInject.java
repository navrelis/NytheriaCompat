// TRACKED HASH: cd8ee0093793e18602ef6a39ab262eaddd098eed
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui.screens.inventory;

import com.google.common.collect.Ordering;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(EffectRenderingInventoryScreen.class)
public abstract class EffectRenderingInventoryScreenInject<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    public EffectRenderingInventoryScreenInject(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @WrapOperation(method = "renderEffects", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Ordering;sortedCopy(Ljava/lang/Iterable;)Ljava/util/List;", remap = false))
    private List<MobEffectInstance> nytheriacompat$filterOnlyRenderableEffects(Ordering<MobEffectInstance> instance, Iterable<MobEffectInstance> elements, Operation<List<MobEffectInstance>> original) {
        return original.call(instance, elements).stream().filter(ClientHooks::shouldRenderEffect).sorted().collect(Collectors.toList());
    }

    @Inject(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/EffectRenderingInventoryScreen;getEffectName(Lnet/minecraft/world/effect/MobEffectInstance;)Lnet/minecraft/network/chat/Component;"))
    private void nytheriacompat$customRenderIconForge(GuiGraphics guiGraphics, int renderX, int yOffset, Iterable<MobEffectInstance> effects, CallbackInfo ci, @Local MobEffectInstance effectInstance, @Local(ordinal = 2) LocalIntRef i) {
        var renderer = IClientMobEffectExtensions.of(effectInstance);

        if (renderer.renderInventoryText(effectInstance, (EffectRenderingInventoryScreen<?>) (Object) this, guiGraphics, renderX, i.get(), 0)) {
            i.set(i.get() + yOffset);
        }
    }

}