/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.injections.client.MouseHandlerInjection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerInject implements MouseHandlerInjection {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Shadow @Final private Minecraft minecraft;

    @WrapOperation(method = "method_1611", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(DDI)Z"))
    private static boolean nytheriacompat$callScreenMouseClickEvents(Screen instance, double mouseX, double mouseY, int button, Operation<Boolean> original) {
        var value = ClientHooks.onScreenMouseClickedPre(instance, mouseX, mouseY, button);

        if (!value) {
            value = original.call(instance, mouseX, mouseY, button);
            value = ClientHooks.onScreenMouseClickedPost(instance, mouseX, mouseY, button, value);
        }

        return value;
    }

    @ModifyExpressionValue(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private <T> T nytheriacompat$getTurnPlayerValuesEvent(T original, @Share("event") LocalRef<CalculatePlayerTurnEvent> eventRef) {
        if (original instanceof Double d) {
            eventRef.set(ClientHooks.getTurnPlayerValues(d, this.minecraft.options.smoothCamera));
            return (T) (Object) eventRef.get().getMouseSensitivity();
        }

        return original;
    }

    @Override
    public double getXVelocity() {
        return this.accumulatedDX;
    }

    @Override
    public double getYVelocity() {
        return this.accumulatedDY;
    }
}
