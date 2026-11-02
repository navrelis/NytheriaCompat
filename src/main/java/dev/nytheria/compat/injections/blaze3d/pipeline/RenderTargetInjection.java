/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.blaze3d.pipeline;

import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface RenderTargetInjection {
    default void enableStencil() {
        throw NytheriaCompatHelper.createMixinException(RenderTargetInjection.class, "enableStencil");
    }

    default boolean isStencilEnabled() {
        throw NytheriaCompatHelper.createMixinException(RenderTargetInjection.class, "isStencilEnabled");
    }
}
