/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.storage.loot;

import io.github.fabricators_of_create.porting_lib.loot.extensions.LootPoolExtensions;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.*;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.world.level.storage.loot.LootPoolInjection;

import java.util.Optional;

@Mixin(LootPool.class)
public abstract class LootPoolInject implements LootPoolInjection, LootPoolExtensions {
    @Shadow @Final @Mutable public NumberProvider rolls;
    @Shadow @Final @Mutable public NumberProvider bonusRolls;

    LootPoolInject(LootPoolEntryContainer[] entries, LootItemCondition[] conditions, LootItemFunction[] functions, NumberProvider rolls, NumberProvider bonusRolls) {}

    @CreateInitializer
    LootPoolInject(LootPoolEntryContainer[] entries, LootItemCondition[] conditions, LootItemFunction[] functions, NumberProvider rolls, NumberProvider bonusRolls, Optional<String> name) {
        this(entries, conditions, functions, rolls, bonusRolls);
        this.setName(name.orElse(null));
    }

    @Unique private boolean isFrozen = false;

    @Override
    public void freeze() {
        this.isFrozen = true;
    }

    @Override
    public boolean isFrozen() {
        return isFrozen;
    }

    @Override
    public NumberProvider getRolls() {
        return this.rolls;
    }

    @Override
    public NumberProvider getBonusRolls() {
        return bonusRolls;
    }

}
