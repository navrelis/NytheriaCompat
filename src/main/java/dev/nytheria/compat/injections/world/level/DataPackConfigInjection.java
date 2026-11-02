/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level;

import net.minecraft.world.level.DataPackConfig;
import dev.nytheria.compat.processor.FabricInjectedInterface;

import java.util.List;

@FabricInjectedInterface(DataPackConfig.class)
public interface DataPackConfigInjection {
    default void addModPacks(List<String> modPacks) {
        throw new IllegalStateException();
    }
}
