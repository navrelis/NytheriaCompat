// TRACKED HASH: af62a88eefa98a16ebc1fc0d8df684f0103f4686
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.biome;

import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.world.level.biome.MobSpawnSettingsInjection;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Mixin(MobSpawnSettings.class)
public abstract class MobSpawnSettingsInject implements MobSpawnSettingsInjection {
    @Shadow @Final private Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>> spawners;
    @Shadow @Final private Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts;

    @Unique private Set<MobCategory> typesView;
    @Unique private Set<EntityType<?>> costView;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nytheriacompat$initViews(float creatureGenerationProbability, Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>> spawners, Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts, CallbackInfo ci) {
        this.typesView = Collections.unmodifiableSet(this.spawners.keySet());
        this.costView = Collections.unmodifiableSet(this.mobSpawnCosts.keySet());
    }

    @Unique
    private void nytheriacompat$tryRebuildViews() {
        if (this.typesView.hashCode() != this.spawners.hashCode()) {
            this.typesView = Collections.unmodifiableSet(this.spawners.keySet());
        }

        if (this.costView.hashCode() != this.mobSpawnCosts.hashCode()) {
            this.costView = Collections.unmodifiableSet(this.mobSpawnCosts.keySet());
        }
    }

    @Override
    public Set<MobCategory> getSpawnerTypes() {
        this.nytheriacompat$tryRebuildViews();
        return this.typesView;
    }

    @Override
    public Set<EntityType<?>> getEntityTypes() {
        this.nytheriacompat$tryRebuildViews();
        return this.costView;
    }
}