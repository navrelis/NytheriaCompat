// TRACKED HASH: 8052166e9529780ad90ee8b00eda7d0ee8ffc2ca
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.material;

import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IFluidExtension;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.util.NytheriaCompatHelper;
import dev.nytheria.compat.workarounds.FluidWorkaround;

import net.minecraft.world.level.material.Fluid;

@Implements(@Interface(iface = FluidWorkaround.class, prefix = "nytheriacompat$i$"))
@Mixin(Fluid.class)
public abstract class FluidInject implements IFluidExtension, FluidWorkaround {
    private FluidType forgeFluidType;

    // Nytheria Compat: called for every entity every tick (and by pathfinding), resolve the override check only once.
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$GET_FLUID_TYPE_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.withReturnType(Fluid.class, "getFluidType", FluidType.class);

    @NotNull
    @Override
    public FluidType neo$getFluidType() {
        // Nytheria Compat: We pray that this works
        if (nytheriacompat$GET_FLUID_TYPE_OVERRIDE.test(this.getClass())) {
            return this.getFluidType();
        }

        if (forgeFluidType == null)
            forgeFluidType = CommonHooks.getVanillaFluidType((Fluid) (Object) this);

        return forgeFluidType;
    }

    @Intrinsic
    public FluidType nytheriacompat$i$getFluidType() {
        return this.neo$getFluidType();
    }
}
