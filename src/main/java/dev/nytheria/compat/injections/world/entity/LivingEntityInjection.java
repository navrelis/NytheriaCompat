/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.Stack;

public interface LivingEntityInjection {
    default Stack<DamageContainer> nytheriacompat$getDamageContainers() {
        throw NytheriaCompatHelper.createMixinException(LivingEntityInjection.class, "nytheriacompat$getDamageContainers");
    }

    default boolean removeEffectsCuredBy(EffectCure cure) {
        throw NytheriaCompatHelper.createMixinException(LivingEntityInjection.class, "removeEffectsCuredBy");
    }

    default boolean shouldRiderFaceForward(Player player) {
        throw NytheriaCompatHelper.createMixinException(LivingEntityInjection.class, "shouldRiderFaceForward");
    }

    // Nytheria Compat: used to report deaths of NeoForge mod entities that override die() without calling super to Fabric
    default void nytheriacompat$setPendingDeathEvent(LivingDeathEvent event) {
        throw NytheriaCompatHelper.createMixinException(LivingEntityInjection.class, "nytheriacompat$setPendingDeathEvent");
    }

    default void nytheriacompat$markFabricDeathReported() {
        throw NytheriaCompatHelper.createMixinException(LivingEntityInjection.class, "nytheriacompat$markFabricDeathReported");
    }

    // Nytheria Compat: posts LivingDeathEvent from inside vanilla LivingEntity/Player/ServerPlayer.die, see ArchitecturyLivingDeathBridge
    default boolean nytheriacompat$postVanillaLivingDeath(DamageSource source) {
        throw NytheriaCompatHelper.createMixinException(LivingEntityInjection.class, "nytheriacompat$postVanillaLivingDeath");
    }

    default boolean nytheriacompat$isPostingVanillaLivingDeath() {
        throw NytheriaCompatHelper.createMixinException(LivingEntityInjection.class, "nytheriacompat$isPostingVanillaLivingDeath");
    }
}
