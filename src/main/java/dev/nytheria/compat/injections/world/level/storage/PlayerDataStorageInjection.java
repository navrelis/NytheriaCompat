/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.storage;

import net.minecraft.world.level.storage.PlayerDataStorage;
import dev.nytheria.compat.processor.FabricInjectedInterface;

import java.io.File;

@FabricInjectedInterface(PlayerDataStorage.class)
public interface PlayerDataStorageInjection {
    default File getPlayerDir() {
        throw new RuntimeException("mixin, why didn't you add this");
    }
}