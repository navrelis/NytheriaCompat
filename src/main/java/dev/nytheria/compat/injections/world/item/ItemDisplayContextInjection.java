/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item;

import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.world.item.ItemDisplayContext;

public interface ItemDisplayContextInjection {
    default boolean isModded() {
        return false;
    }

    default @Nullable ItemDisplayContext fallback() {
        throw NytheriaCompatHelper.createMixinException(ItemDisplayContextInjection.class, "fallback");
    }

    default void nytheriacompat$markModded() {
        throw NytheriaCompatHelper.createMixinException(ItemDisplayContextInjection.class, "nytheriacompat$markModded");
    }

    default void nytheriacompat$setFallback(ItemDisplayContext fallback) {
        throw NytheriaCompatHelper.createMixinException(ItemDisplayContextInjection.class, "nytheriacompat$setFallback");
    }
}
