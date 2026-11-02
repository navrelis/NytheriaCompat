// TRACKED HASH: 31fa78f22b9de5f08b03a4be3162c7c3334e121f
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.inventory;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.hook.Scope;
import dev.nytheria.compat.injections.world.inventory.AnvilMenuInjection;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuInject extends ItemCombinerMenu implements AnvilMenuInjection {
    @Shadow @Final private DataSlot cost;
    @Shadow private @Nullable String itemName;

    public AnvilMenuInject(@Nullable MenuType<?> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isDamageableItem()Z"))
    private void nytheriacompat$storeEnchantedBookFlag(CallbackInfo ci, @Local boolean flag, @Share("flag") LocalBooleanRef flagRef) {
        flagRef.set(flag);
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V", ordinal = 5))
    private void nytheriacompat$checkAndSetFlag(CallbackInfo ci, @Share("flag") LocalBooleanRef flagRef, @Local(ordinal = 1) LocalRef<ItemStack> stack2, @Local(ordinal = 2) ItemStack stack3) {
        if (flagRef.get() && Scope.isNeoObject(stack2.get().getItem()) && !stack2.get().isBookEnchantable(stack3))
            stack2.set(ItemStack.EMPTY);
    }

    @Override
    public void setMaximumCost(long value) {
        this.cost.set((int) Mth.clamp(value, 0L, Integer.MAX_VALUE));
    }
}