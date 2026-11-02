/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import dev.nytheria.compat.hook.Scope;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.neoforge.common.ItemAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BambooStalkBlock.class)
public abstract class BambooStalkBlockInject extends Block {
    public BambooStalkBlockInject(Properties properties) {
        super(properties);
    }

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$checkCanSustainPlant(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var soil = level.getBlockState(pos.below());
        if (!Scope.isNeoObject((Object) soil.getBlock()))
            return;

        var soilDecision = soil.canSustainPlant(level, pos.below(), Direction.UP, state);
        if (!soilDecision.isDefault())
            cir.setReturnValue(soilDecision.isTrue());
    }

    @WrapOperation(method = "getDestroyProgress", constant = @Constant(classValue = SwordItem.class))
    private boolean nytheriacompat$checkCanPerformSwordDig(Object object, Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        return original.call(object) || (Scope.isNeoObject(player.getMainHandItem().getItem()) && player.getMainHandItem().canPerformAction(ItemAbilities.SWORD_DIG));
    }
}
