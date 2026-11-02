/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.ai.behavior;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(GiveGiftToHero.class)
public abstract class GiveGiftToHeroInject {
    @ModifyExpressionValue(method = "getItemToThrow", at = @At(value = "INVOKE", target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z"))
    private boolean nytheriacompat$justAlwaysReturnTrue(boolean original, @Local VillagerProfession profession) {
        return original || BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(profession).getData(NeoForgeDataMaps.RAID_HERO_GIFTS) != null;
    }

    @WrapOperation(method = "getItemToThrow", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private <K, V> V nytheriacompat$tryUseDataMap(Map<K, V> instance, Object o, Operation<V> original, @Local VillagerProfession profession) {
        var existing = original.call(instance, o);

        if (existing != null)
            return existing;

        var gift = BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(profession).getData(NeoForgeDataMaps.RAID_HERO_GIFTS);

        if (gift != null) {
            return (V) gift.lootTable();
        }

        return null;
    }
}
