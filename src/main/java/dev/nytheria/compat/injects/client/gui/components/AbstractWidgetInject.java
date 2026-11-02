// TRACKED HASH: 5e539bec12a4a4d92962df738afd4a7595db2eeb
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.gui.components;

import net.neoforged.neoforge.client.extensions.IAbstractWidgetExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.client.gui.components.AbstractWidgetInjection;

import net.minecraft.client.gui.components.AbstractWidget;

@Mixin(AbstractWidget.class)
public class AbstractWidgetInject implements AbstractWidgetInjection, IAbstractWidgetExtension {
    @Shadow public boolean active;

    protected int packedFGColor = AbstractWidgetInjection.UNSET_FG_COLOR;

    @CreateStatic
    private static final int UNSET_FG_COLOR = AbstractWidgetInjection.UNSET_FG_COLOR;

    @Override
    public int getFGColor() {
        if (packedFGColor != AbstractWidgetInjection.UNSET_FG_COLOR)
            return packedFGColor;

        return this.active ? 16777215 : 10526880; // white : light grey
    }

}
