/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Supplier;

public interface FlowerPotBlockInjection {
    default FlowerPotBlock getEmptyPot() {
        throw new IllegalStateException();
    }

    default Map<ResourceLocation, Supplier<? extends Block>> getFullPotsView() {
        throw new IllegalStateException();
    }

    static FlowerPotBlock create(@Nullable Supplier<FlowerPotBlock> emptyPot, Supplier<? extends Block> block, BlockBehaviour.Properties properties) {
        try {
            var initializer = FlowerPotBlock.class.getDeclaredConstructor(Supplier.class, Supplier.class, BlockBehaviour.Properties.class);

            return initializer.newInstance(emptyPot, block, properties);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
