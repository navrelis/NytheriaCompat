/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.monster;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnderMan.class)
public abstract class EnderManInject extends Monster {
    protected EnderManInject(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    // TODO: do we need to worry about data manager issues?

    @ModifyExpressionValue(method = "isLookingAtMe", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z", ordinal = 0))
    private boolean nytheriacompat$checkShouldSuppressAnger(boolean original, @Local(argsOnly = true) Player player, @Local ItemStack stack) {
        return original || CommonHooks.shouldSuppressEnderManAnger((EnderMan) (Object) this, player, stack);
    }

    @Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal")
    public abstract static class EndermanLeaveBlockGoalInject extends Goal {
        @Shadow @Final private EnderMan enderman;

        @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/EnderMan$EndermanLeaveBlockGoal;canPlaceBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)Z"))
        private boolean nytheriacompat$checkCanPlaceBlockEvent(boolean original, @Local Level level, @Local(ordinal = 1) BlockPos pos1) {
            return original && !EventHooks.onBlockPlace(this.enderman, BlockSnapshot.create(level.dimension(), level, pos1), Direction.UP);
        }
    }

    @Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
    public abstract static class EndermanTakeBlockGoalInject extends Goal {
        @Shadow @Final private EnderMan enderman;

    }
}
