/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.biome;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.Set;

public interface MobSpawnSettingsInjection {
    default Set<MobCategory> getSpawnerTypes() {
        throw NytheriaCompatHelper.createMixinException(MobSpawnSettingsInjection.class, "getSpawnerTypes");
    }

    default Set<EntityType<?>> getEntityTypes() {
        throw NytheriaCompatHelper.createMixinException(MobSpawnSettingsInjection.class, "getEntityTypes");
    }
}
