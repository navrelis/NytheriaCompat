/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.storage;

import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.nio.file.Path;

public interface LevelStorageAccessInjection {
    default void readAdditionalLevelSaveData(boolean fallback) {
        throw NytheriaCompatHelper.createMixinException(LevelStorageAccessInjection.class, "readAdditionalLevelSaveData");
    }

    default Path getWorldDir() {
        throw NytheriaCompatHelper.createMixinException(LevelStorageAccessInjection.class, "getWorldDir");
    }
}
