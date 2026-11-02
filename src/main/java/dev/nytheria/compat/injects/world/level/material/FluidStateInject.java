// TRACKED HASH: da4c7d69266cd226e6baab431327d61c6d9516d3
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.material;

import net.neoforged.neoforge.common.extensions.IFluidStateExtension;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.workarounds.FluidWorkaround;

import net.minecraft.world.level.material.FluidState;

@Implements(@Interface(iface = FluidWorkaround.class, prefix = "nytheriacompat$i$"))
@Mixin(FluidState.class)
public abstract class FluidStateInject implements IFluidStateExtension {
    @Intrinsic
    public FluidType nytheriacompat$i$getFluidType() {
        return this.neo$getFluidType();
    }
}
