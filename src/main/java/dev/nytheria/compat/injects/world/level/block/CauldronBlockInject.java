/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

@Mixin(CauldronBlock.class)
public abstract class CauldronBlockInject extends Block {
    public CauldronBlockInject(Properties properties) {
        super(properties);
    }

    @Inject(method = "receiveStalactiteDrip", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$checkHandleCauldronDrip(BlockState state, Level level, BlockPos pos, Fluid fluid, CallbackInfo ci) {
        // Nytheria Compat: NeoForge's water and lava types handle the drip too, with the same result as vanilla; leave vanilla's
        //  fluids to vanilla's code.
        if (!fluid.neo$getFluidType().isVanilla() && fluid.neo$getFluidType().handleCauldronDrip(fluid, level, pos))
            ci.cancel();
    }
}
