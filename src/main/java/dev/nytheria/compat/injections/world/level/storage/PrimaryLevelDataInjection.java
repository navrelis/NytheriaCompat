/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.storage;

import net.minecraft.world.level.storage.PrimaryLevelData;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface PrimaryLevelDataInjection {
    default boolean hasConfirmedExperimentalWarning() {
        throw NytheriaCompatHelper.createMixinException(PrimaryLevelDataInjection.class, "hasConfirmedExperimentalWarning");
    }

    default PrimaryLevelData withConfirmedWarning(boolean confirmedWarning) {
        throw NytheriaCompatHelper.createMixinException(PrimaryLevelDataInjection.class, "withConfirmedWarning");
    }
}
