/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.helpers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import kotlin.Pair;
import kotlin.Triple;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.util.Quadruple;

import java.time.Duration;

public class CacheOptimizationStorage {
    public static final Cache<Quadruple<IClientItemExtensions, LivingEntity, ItemStack, EquipmentSlot>, Boolean> SHOULD_ACTUALLY_BE_CACHED = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(5L))
        .build();

    public static final Cache<Quadruple<IClientItemExtensions, LivingEntity, ItemStack, EquipmentSlot>, HumanoidModel<?>> ARMOR_MODEL_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(5L))
        .build();
}
