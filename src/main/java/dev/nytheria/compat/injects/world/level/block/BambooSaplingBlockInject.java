// TRACKED HASH: 95299a400ff184f86da97a59ad8fcdab2625753a
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import dev.nytheria.compat.hook.Scope;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.neoforged.neoforge.common.ItemAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;

@Mixin(BambooSaplingBlock.class)
public abstract class BambooSaplingBlockInject {
    @WrapOperation(method = "getDestroyProgress", constant = @Constant(classValue = SwordItem.class))
    private boolean nytheriacompat$checkCanPerformSwordDig(Object object, Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        return original.call(object) || (Scope.isNeoObject(player.getMainHandItem().getItem()) && player.getMainHandItem().canPerformAction(ItemAbilities.SWORD_DIG));
    }
}