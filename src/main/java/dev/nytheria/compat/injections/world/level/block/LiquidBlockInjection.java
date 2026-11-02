/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.block;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.function.Supplier;

public interface LiquidBlockInjection {
    static LiquidBlock create(Supplier<? extends FlowingFluid> fluidSupplier, BlockBehaviour.Properties properties) {
        return new LiquidBlock(fluidSupplier.get(), properties);
    }

    default FlowingFluid getFluid() {
        throw NytheriaCompatHelper.createMixinException(LiquidBlockInjection.class, "getFluid");
    }
}
