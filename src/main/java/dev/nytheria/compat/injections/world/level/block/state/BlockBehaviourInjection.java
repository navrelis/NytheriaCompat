/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.block.state;

import java.util.function.Supplier;

import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

public interface BlockBehaviourInjection {
    default boolean isAir(BlockState state) {
        throw NytheriaCompatHelper.createMixinException(BlockBehaviourInjection.class, "isAir");
    }

    interface PropertiesInjection {
        default Supplier<ResourceKey<LootTable>> getLootTableSupplier() {
            throw new IllegalStateException();
        }
    }

    interface BlockStateBaseInjection {
        default boolean nytheriacompat$isAir() {
            throw NytheriaCompatHelper.createMixinException(BlockStateBaseInjection.class, "nytheriacompat$isAir");
        }
    }
}
