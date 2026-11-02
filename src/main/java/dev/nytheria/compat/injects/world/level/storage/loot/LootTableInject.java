/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.storage.loot;

import io.github.fabricators_of_create.porting_lib.loot.extensions.LootTableExtensions;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.*;
import dev.nytheria.compat.injections.world.level.storage.loot.LootPoolInjection;
import dev.nytheria.compat.injections.world.level.storage.loot.LootTableInjection;

import java.util.List;

@Mixin(LootTable.class)
public abstract class LootTableInject implements LootTableInjection, LootTableExtensions {
    @Shadow @Final @Mutable
    private List<LootPool> pools;
    @Unique private boolean isFrozen = false;

    @Override
    public void freeze() {
        this.isFrozen = true;
        this.pools.forEach(LootPoolInjection::freeze);
    }

    @Override
    public boolean isFrozen() {
        return isFrozen;
    }

}
