/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block.state;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.injections.world.level.block.state.BlockBehaviourInjection;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourInject implements BlockBehaviourInjection {
    @Shadow
    protected abstract Block asBlock();


    public boolean isAir(BlockState state) {
        if (state instanceof BlockStateBaseInjection injection) {
            return injection.nytheriacompat$isAir();
        }

        return state.isAir();
    }

    @Mixin(BlockBehaviour.BlockStateBase.class)
    public abstract static class BlockStateBaseInject implements BlockBehaviourInjection.BlockStateBaseInjection {
        @Shadow
        @Final
        private boolean isAir;


        @Override
        public boolean nytheriacompat$isAir() {
            return this.isAir;
        }

    }

    @Mixin(BlockBehaviour.Properties.class)
    public static abstract class PropertiesInject implements BlockBehaviourInjection.PropertiesInjection {
        @Unique private Supplier<ResourceKey<LootTable>> lootTableSupplier;

        @Override
        public Supplier<ResourceKey<LootTable>> getLootTableSupplier() {
            return this.lootTableSupplier;
        }

    }
}
