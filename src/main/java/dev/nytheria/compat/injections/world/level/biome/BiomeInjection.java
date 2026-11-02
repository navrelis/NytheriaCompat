/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.biome;

import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

public interface BiomeInjection {
    default ModifiableBiomeInfo modifiableBiomeInfo() {
        throw new IllegalStateException();
    }

}
