// TRACKED HASH: 604c32f8a695e1a5a43593eec26599fd0fc2203c
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity;

import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.entity.SpawnPlacementsInjection;

import java.util.Map;

@Mixin(SpawnPlacements.class)
public abstract class SpawnPlacementsInject implements SpawnPlacementsInjection {
    @Shadow @Final public static Map<EntityType<?>, SpawnPlacements.Data> DATA_BY_TYPE;

    @CreateStatic
    private static boolean hasPlacement(EntityType<?> type) {
        return SpawnPlacementsInjection.hasPlacement(type);
    }

    @CreateStatic
    private static void fireSpawnPlacementEvent() {
        SpawnPlacementsInjection.fireSpawnPlacementEvent();
    }
}