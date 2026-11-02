// TRACKED HASH: ef9fd99db3d48c0d70bf22631982a82f03e8d541
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui.components;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.client.gui.components.ButtonBuilderInjection;
import dev.nytheria.compat.injections.client.gui.components.ButtonInjection;
import dev.nytheria.compat.mixin.client.gui.components.ButtonBuilderAccessor;

import java.util.function.Function;

@Mixin(Button.class)
public abstract class ButtonInject extends AbstractButton implements ButtonInjection {
    protected ButtonInject(int x, int y, int width, int height, Component message, Button.OnPress onPress, Button.CreateNarration createNarration) {
        super(x, y, width, height, message);
    }

    @CreateInitializer
    protected ButtonInject(Button.Builder builder) {
        this(((ButtonBuilderAccessor) builder).getX(), ((ButtonBuilderAccessor) builder).getY(), ((ButtonBuilderAccessor) builder).getWidth(), ((ButtonBuilderAccessor) builder).getHeight(), ((ButtonBuilderAccessor) builder).getMessage(), ((ButtonBuilderAccessor) builder).getOnPress(), ((ButtonBuilderAccessor) builder).getCreateNarration());
        this.setTooltip(((ButtonBuilderAccessor) builder).getTooltip());
    }

    @Mixin(Button.Builder.class)
    public static class ButtonBuilderInject implements ButtonBuilderInjection {
        @Override
        public Button build(Function<Button.Builder, Button> builder) {
            return ButtonBuilderInjection.super.build(builder);
        }
    }
}