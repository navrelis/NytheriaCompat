// TRACKED HASH: 463588f2acf28725b91608efe6c2270022c2dde9
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerInject {

    @ModifyExpressionValue(method = "mobsAt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/structures/NetherFortressStructure;FORTRESS_ENEMIES:Lnet/minecraft/util/random/WeightedRandomList;"))
    private static WeightedRandomList<MobSpawnSettings.SpawnerData> nytheriacompat$tryUseNeoMonsterSpawns(WeightedRandomList<MobSpawnSettings.SpawnerData> original, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) MobCategory category, @Local(argsOnly = true) BlockPos pos) {
        var monsterSpawns = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
            .getOrThrow(BuiltinStructures.FORTRESS)
            .spawnOverrides()
            .get(MobCategory.MONSTER);

        // TODO: this might be mod-incompatible...
        if (monsterSpawns != null) {
            return EventHooks.getPotentialSpawns(level, category, pos, monsterSpawns.spawns());
        }

        return EventHooks.getPotentialSpawns(level, category, pos, original);
    }
}