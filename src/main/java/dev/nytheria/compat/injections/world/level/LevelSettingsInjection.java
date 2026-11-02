/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level;

import com.mojang.serialization.Lifecycle;
import net.minecraft.world.level.LevelSettings;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface LevelSettingsInjection {
    default LevelSettings withLifecycle(Lifecycle lifecycle) {
        throw NytheriaCompatHelper.createMixinException(LevelSettingsInjection.class, "withLifecycle");
    }

    default Lifecycle getLifecycle() {
        throw NytheriaCompatHelper.createMixinException(LevelSettingsInjection.class, "getLifecycle");
    }

    default void nytheriacompat$setLifecycle(Lifecycle lifecycle) {
        throw NytheriaCompatHelper.createMixinException(LevelSettingsInjection.class, "nytheriacompat$setLifecycle");
    }
}
