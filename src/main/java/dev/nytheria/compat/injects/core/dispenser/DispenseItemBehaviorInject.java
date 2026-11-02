/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.core.dispenser;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.hook.Scope;

@Mixin(DispenseItemBehavior.class)
public interface DispenseItemBehaviorInject {
    @Mixin(targets = "net/minecraft/core/dispenser/DispenseItemBehavior$8")
    public abstract static class FlintAndSteelDispenseItemBehaviorInject {
        @Definition(id = "blockState", local = @Local(type = BlockState.class))
        @Definition(id = "getBlock", method = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;")
        @Definition(id = "TntBlock", type = TntBlock.class)
        @Expression("blockState.getBlock() instanceof TntBlock")
        @ModifyExpressionValue(method = "execute", at = @At("MIXINEXTRAS:EXPRESSION"))
        private boolean nytheriacompat$checkIsFlammable(boolean original, @Local BlockState state, @Local ServerLevel level, @Local BlockPos pos, @Local(argsOnly = true) BlockSource source) {
            if (!original) {
                if (Scope.isNeoObject(state.getBlock()) && state.isFlammable(level, pos, source.state().getValue(DispenserBlock.FACING).getOpposite())) {
                    return true;
                }
            }

            return original;
        }
    }
}
