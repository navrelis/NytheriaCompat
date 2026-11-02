/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.compat.porting_lib;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FluidType.class)
public abstract class FluidTypeMixin {
    @ModifyReturnValue(method = "isAir", at = @At("RETURN"), remap = false)
    private boolean nytheriacompat$checkIsForgeAir(boolean original) {
        return original || (Object) this == NeoForgeMod.EMPTY_TYPE.value();
    }

    @Definition(id = "WATER_TYPE", field = "Lio/github/fabricators_of_create/porting_lib/fluids/PortingLibFluids;WATER_TYPE:Lio/github/fabricators_of_create/porting_lib/fluids/FluidType;")
    @Expression("this == WATER_TYPE")
    @ModifyExpressionValue(method = {"canRideVehicleUnder", "isVaporizedOnPlacement"}, at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$checkIsForgeWater(boolean original) {
        return original || (Object) this == NeoForgeMod.WATER_TYPE.value();
    }

    @ModifyReturnValue(method = "isVanilla", at = @At("RETURN"), remap = false)
    private boolean nytheriacompat$checkIsForgeVanillaFluid(boolean original) {
        return original || (Object) this == NeoForgeMod.WATER_TYPE.value() || (Object) this == NeoForgeMod.LAVA_TYPE.value();
    }
}
