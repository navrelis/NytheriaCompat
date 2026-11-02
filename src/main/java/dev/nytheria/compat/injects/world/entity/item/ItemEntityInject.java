/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.item;

import java.util.UUID;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.world.entity.item.ItemEntityInjection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(ItemEntity.class)
public abstract class ItemEntityInject extends Entity implements ItemEntityInjection {
    public ItemEntityInject(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow @Final private static int LIFETIME;
    @Shadow public abstract ItemStack getItem();

    @Shadow
    @Nullable
    private UUID target;
    @Shadow
    private int age;
    public int lifespan = LIFETIME;

    @Override
    public int nytheriacompat$getLifespan() {
        return lifespan;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;DDD)V", at = @At("TAIL"))
    private void nytheriacompat$initLifespan(Level level, double posX, double posY, double posZ, ItemStack stack, double deltaX, double deltaY, double deltaZ, CallbackInfo ci) {
        this.lifespan = (stack.getItem() == null ? LIFETIME : stack.getEntityLifespan(level));
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/item/ItemEntity;)V", at = @At("TAIL"))
    private void nytheriacompat$initLifespanFromOther(ItemEntity other, CallbackInfo ci) {
        this.lifespan = other.nytheriacompat$getLifespan();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;isInWater()Z"))
    private void nytheriacompat$handleFluidCollision(CallbackInfo ci, @Share("hasHandledFluid") LocalBooleanRef hasHandledFluid) {
        var fluidType = this.getMaxHeightFluidType();
        if (!fluidType.isAir() && !fluidType.isVanilla() && this.getFluidTypeHeight(fluidType) > 0.1f) {
            fluidType.setItemMovement((ItemEntity) (Object) this);
            hasHandledFluid.set(true);
        }
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;applyGravity()V", ordinal = 0))
    private boolean nytheriacompat$cancelIfHasHandledFluid(ItemEntity instance, @Share("hasHandledFluid") LocalBooleanRef hasHandledFluid) {
        return !hasHandledFluid.get();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V", ordinal = 1))
    private void nytheriacompat$checkItemExpireEvent(ItemEntity instance, Operation<Void> original) {
        this.lifespan = Mth.clamp(this.lifespan + EventHooks.onItemExpire(instance), 0, Short.MAX_VALUE - 1);

        if (this.age >= this.lifespan) {
            original.call(instance);
        }
    }

    // Nytheria Compat: IItemExtension#onDestroyed takes (ItemEntity, DamageSource); the ItemStack parameter this lookup used to list matched no method, so a NeoForge override was never honoured.

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void nytheriacompat$loadLifespanData(CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("Lifespan")) {
            this.lifespan = compound.getInt("Lifespan");
        }
    }

    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void nytheriacompat$checkPickupEvent(Player player, CallbackInfo ci, @Share("copy") LocalRef<ItemStack> copyRef, @Local ItemStack stack) {
        var result = EventHooks.fireItemPickupPre((ItemEntity) (Object) this, player).canPickup();
        if (result.isFalse()) {
            ci.cancel();
            return;
        }

        copyRef.set(stack.copy());
    }

    @ModifyVariable(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V", ordinal = 0, shift = At.Shift.BEFORE), ordinal = 0)
    private int nytheriacompat$callPlayerItemTouchEvent(int count, @Share("copy") LocalRef<ItemStack> copyRef, @Local ItemStack stack, @Local(argsOnly = true) Player player) {
        EventHooks.fireItemPickupPost((ItemEntity) (Object) this, player, copyRef.get());
        return copyRef.get().getCount() - stack.getCount();
    }

    @Override
    public @Nullable UUID getTarget() {
        return this.target;
    }

}
