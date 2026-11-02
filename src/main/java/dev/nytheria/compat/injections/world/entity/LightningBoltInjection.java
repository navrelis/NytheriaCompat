/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.entity;

import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface LightningBoltInjection {
    default void setDamage(float damage) {
        throw NytheriaCompatHelper.createMixinException(LightningBoltInjection.class, "setDamage");
    }

    default float getDamage() {
        throw NytheriaCompatHelper.createMixinException(LightningBoltInjection.class, "getDamage");
    }
}
