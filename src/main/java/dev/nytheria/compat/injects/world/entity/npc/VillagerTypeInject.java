/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.npc;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.helpers.mixin.CreateStatic;

import java.util.Map;

@Mixin(VillagerType.class)
public abstract class VillagerTypeInject {
    @Shadow @Final public static Map<ResourceKey<Biome>, VillagerType> BY_BIOME;

    @CreateStatic
    private static void registerBiomeType(ResourceKey<Biome> biomeKey, VillagerType villagerType) {
        BY_BIOME.put(biomeKey, villagerType);
    }
}
