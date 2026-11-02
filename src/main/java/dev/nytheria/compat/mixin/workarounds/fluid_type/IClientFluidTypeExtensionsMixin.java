/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.workarounds.fluid_type;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.fabricators_of_create.porting_lib.fluids.PortingLibFluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.helpers.StupidWorkarounds;
import dev.nytheria.compat.workarounds.FabricFluidTypeExtensions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

@Mixin(IClientFluidTypeExtensions.class)
public interface IClientFluidTypeExtensionsMixin {
    @WrapOperation(method = {"of(Lnet/minecraft/world/level/material/Fluid;)Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;"}, at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;of(Lnet/neoforged/neoforge/fluids/FluidType;)Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;"))
    private static IClientFluidTypeExtensions nytheriacompat$tryHandleFabricFluidTypes(FluidType type, Operation<IClientFluidTypeExtensions> original, @Local(argsOnly = true) Fluid fluid) {
        if (type.nytheriacompat$isWrapped) {
            return StupidWorkarounds.nytheriacompat$fabricFluidExtensions.computeIfAbsent(type, $ -> new FabricFluidTypeExtensions(fluid));
        }

        return original.call(type);
    }

    @WrapOperation(method = {"of(Lnet/minecraft/world/level/material/FluidState;)Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;"}, at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;of(Lnet/neoforged/neoforge/fluids/FluidType;)Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;"))
    private static IClientFluidTypeExtensions nytheriacompat$tryHandleFabricFluidTypes(FluidType type, Operation<IClientFluidTypeExtensions> original, @Local(argsOnly = true) FluidState state) {
        if (type.nytheriacompat$isWrapped) {
            return StupidWorkarounds.nytheriacompat$fabricFluidExtensions.computeIfAbsent(type, $ -> new FabricFluidTypeExtensions(state.getType()));
        }

        return original.call(type);
    }

    @Inject(method = "of(Lnet/neoforged/neoforge/fluids/FluidType;)Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;", at = @At("HEAD"), cancellable = true)
    private static void nytheriacompat$tryHandleFabricFluidTypes(FluidType type, CallbackInfoReturnable<IClientFluidTypeExtensions> cir) {
        if (type.nytheriacompat$isWrapped) {
            if (StupidWorkarounds.nytheriacompat$fabricFluidExtensions.containsKey(type)) {
                cir.setReturnValue(StupidWorkarounds.nytheriacompat$fabricFluidExtensions.get(type));
                return;
            }

            // Nytheria Compat: Try to derive the fluid type
            if (type.nytheriacompat$wrapped != null) {
                var key = PortingLibFluids.FLUID_TYPES.getKey(type.nytheriacompat$wrapped);
                var fluid = BuiltInRegistries.FLUID.getOptional(key).orElse(null);

                if (fluid != null) {
                    var fabricExt = new FabricFluidTypeExtensions(fluid);
                    StupidWorkarounds.nytheriacompat$fabricFluidExtensions.put(type, fabricExt);

                    cir.setReturnValue(fabricExt);
                    StupidWorkarounds.nytheriacompat$fabricFluidExtensions.put(type, fabricExt);
                    return;
                }
            }
        }
    }
}
