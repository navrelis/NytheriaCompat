/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.particle;

import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface ParticleRenderTypeInjection {
    default boolean isTranslucent() {
        throw NytheriaCompatHelper.createMixinException(ParticleRenderTypeInjection.class, "isTranslucent");
    }
}
