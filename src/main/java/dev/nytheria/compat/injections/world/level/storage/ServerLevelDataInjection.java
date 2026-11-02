/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.storage;

import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface ServerLevelDataInjection {
    default void setDayTimeFraction(float dayTimeFraction) {
        throw NytheriaCompatHelper.createMixinException(ServerLevelDataInjection.class, "setDayTimeFraction");
    }

    default float getDayTimeFraction() {
        throw NytheriaCompatHelper.createMixinException(ServerLevelDataInjection.class, "getDayTimeFraction");
    }

    default float getDayTimePerTick() {
        throw NytheriaCompatHelper.createMixinException(ServerLevelDataInjection.class, "getDayTimePerTick");
    }

    default void setDayTimePerTick(float dayTimePerTick) {
        throw NytheriaCompatHelper.createMixinException(ServerLevelDataInjection.class, "setDayTimePerTick");
    }
}
