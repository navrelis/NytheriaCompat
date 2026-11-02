// TRACKED HASH: b73c34d168c602ac81d45109572d392b4ad484f8
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.hook.Scope;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererInject {
    @Shadow @Final private Minecraft minecraft;

    @Shadow private ItemStack mainHandItem;

    @Shadow private ItemStack offHandItem;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F", shift = At.Shift.AFTER))
    private void nytheriacompat$initReequipAnim(CallbackInfo ci, @Share("reequipM") LocalBooleanRef reequipM, @Share("reequipO") LocalBooleanRef reequipO, @Local(ordinal = 0) ItemStack stack, @Local(ordinal = 1) ItemStack offhandStack, @Local LocalPlayer player) {
        reequipM.set(!Scope.isNeoObject(this.mainHandItem.getItem()) || ClientHooks.shouldCauseReequipAnimation(this.mainHandItem, stack, player.getInventory().selected));
        reequipO.set(!Scope.isNeoObject(this.offHandItem.getItem()) || ClientHooks.shouldCauseReequipAnimation(this.offHandItem, offhandStack, -1));

        if (!reequipM.get() && this.mainHandItem != stack)
            this.mainHandItem = stack;

        if (!reequipO.get() && this.offHandItem != offhandStack)
            this.offHandItem = offhandStack;
    }

	@Unique
	private static boolean nytheriacompat$shouldSkipReequip(boolean fabricSkipReequip, boolean forgeSkipReequip) {
		if (forgeSkipReequip) {
			return true;
		}
		return fabricSkipReequip;
	}

    @Definition(id = "mainHandItem", field = "Lnet/minecraft/client/renderer/ItemInHandRenderer;mainHandItem:Lnet/minecraft/world/item/ItemStack;")
    @Definition(id = "itemStack", local = @Local(type = ItemStack.class, ordinal = 0))
    @Expression("this.mainHandItem == itemStack")
    @ModifyExpressionValue(method = "tick", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$useReequipCheckForMainHand(boolean original, @Share("reequipM") LocalBooleanRef reequipM) {
		return nytheriacompat$shouldSkipReequip(original, !reequipM.get());
    }

	@Definition(id = "offHandItem", field = "Lnet/minecraft/client/renderer/ItemInHandRenderer;offHandItem:Lnet/minecraft/world/item/ItemStack;")
	@Definition(id = "itemStack2", local = @Local(type = ItemStack.class, ordinal = 1))
	@Expression("this.offHandItem == itemStack2")
	@ModifyExpressionValue(method = "tick", at = @At("MIXINEXTRAS:EXPRESSION"))
	private boolean nytheriacompat$useReequipCheckForOffHand(boolean original, @Share("reequipO") LocalBooleanRef reequipO) {
		return nytheriacompat$shouldSkipReequip(original, !reequipO.get());
	}

}