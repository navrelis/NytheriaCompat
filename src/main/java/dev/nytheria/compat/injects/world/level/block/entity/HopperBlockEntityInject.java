// TRACKED HASH: debbd63809934e4b473f32625f60d1448d5c238d
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.VanillaInventoryCodeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.world.level.block.entity.HopperBlockEntityInjection;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityInject extends RandomizableContainerBlockEntity implements HopperBlockEntityInjection {
    @Shadow private long tickedGameTime;

    protected HopperBlockEntityInject(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
    private static void nytheriacompat$insertEjectHook(Level level, BlockPos pos, HopperBlockEntity hopper, CallbackInfoReturnable<Boolean> cir) {
        if (VanillaInventoryCodeHooks.insertHook(hopper))
            cir.setReturnValue(true);
    }

    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void nytheriacompat$insertExtractHook(Level level, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        var ret = VanillaInventoryCodeHooks.extractHook(level, hopper);

        if (ret != null)
            cir.setReturnValue(ret);
    }

    @Override
    public long getLastUpdateTime() {
        return this.tickedGameTime;
    }
}