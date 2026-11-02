/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item;

import net.minecraft.network.chat.Style;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.function.UnaryOperator;

public interface RarityInjection {

    default UnaryOperator<Style> getStyleModifier() {
        throw NytheriaCompatHelper.createMixinException(RarityInjection.class, "getStyleModifier");
    }

    default boolean nytheriacompat$hasCustomStyleModifier() {
        throw NytheriaCompatHelper.createMixinException(RarityInjection.class, "nytheriacompat$hasCustomStyleModifier");
    }

}
