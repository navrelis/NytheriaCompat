/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.entity;

import net.minecraft.world.entity.MobSpawnType;

public interface MobInjection {
    boolean isSpawnCancelled();
    void setSpawnCancelled(boolean cancel);

    MobSpawnType getSpawnType();
}
