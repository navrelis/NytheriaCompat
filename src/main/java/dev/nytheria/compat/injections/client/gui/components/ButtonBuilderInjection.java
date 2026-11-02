/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.gui.components;

import net.minecraft.client.gui.components.Button;

import java.util.function.Function;

public interface ButtonBuilderInjection {
    default Button build(Function<Button.Builder, Button> builder) {
        return builder.apply((Button.Builder) this);
    }
}
