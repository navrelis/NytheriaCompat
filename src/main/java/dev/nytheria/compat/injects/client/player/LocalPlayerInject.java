// TRACKED HASH: 4a532f665aabef3c36a9818c82e846213791c292
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.hook.NeoFluids;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerInject extends AbstractClientPlayer {
    public LocalPlayerInject(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Shadow public abstract boolean isUsingItem();
    @Shadow public abstract InteractionHand getUsedItemHand();
    @Shadow public abstract void stopUsingItem();

    @Shadow public Input input;

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/Tutorial;onInput(Lnet/minecraft/client/player/Input;)V"))
    private void nytheriacompat$handleForgeMovementUpdate(CallbackInfo ci) {
        ClientHooks.onMovementInputUpdate(this, this.input);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z", ordinal = 0))
    private boolean nytheriacompat$checkCanSwim(boolean original) {
        return original || (NeoFluids.isNeoType(this.getEyeInFluidType()) && this.canStartSwimming());
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z", ordinal = 1))
    private boolean nytheriacompat$checkCanSwim2(boolean original) {
        return original || (NeoFluids.isNeoType(this.getEyeInFluidType()) && this.canStartSwimming());
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isInWater()Z", ordinal = 0))
    private boolean nytheriacompat$checkCanSwimInFluidType(boolean original) {
        return original || (this.isInFluidType() && this.isInFluidType((fluidType, height) -> this.canSwimInFluidType(fluidType)));
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z", ordinal = 2))
    private boolean nytheriacompat$checkCanSwimAndInFluidType(boolean original) {
        return original && !(this.isInFluidType((fluidType, height) -> this.canSwimInFluidType(fluidType)) && !this.canStartSwimming());
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isInWater()Z", ordinal = 2))
    private boolean nytheriacompat$checkCanSwimInFluidType2(boolean original) {
        return original || (this.isInFluidType() && this.isInFluidType((fluidType, height) -> this.canSwimInFluidType(fluidType)));
    }

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean nytheriacompat$checkCanElytraFly(ItemStack instance, Item item, Operation<Boolean> original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(instance.getItem().getClass(), IItemExtension.class, "canElytraFly", ItemStack.class, LivingEntity.class)) {
            return instance.canElytraFly(this);
        }

        return original.call(instance, item);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isInWater()Z", ordinal = 3))
    private boolean nytheriacompat$checkCanSwimInFluidType3(boolean original, @Share("fluidType") LocalRef<FluidType> fluidType) {
        fluidType.set(this.getMaxHeightFluidType());
        return original || (!fluidType.get().isAir() && this.canSwimInFluidType(fluidType.get()));
    }

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;goDownInWater()V"))
    private void nytheriacompat$trySinkInFluid(LocalPlayer instance, Operation<Void> original, @Share("fluidType") LocalRef<FluidType> fluidType) {
        if (fluidType.get() == null /*|| fluidType.get().nytheriacompat$isWrapped*/ || fluidType.get().isVanilla()) {
            original.call(instance);
        } else {
            this.sinkInFluid(fluidType.get());
        }
    }

    @Inject(method = "rideTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;rideTick()V", shift = At.Shift.AFTER))
    private void nytheriacompat$disableShiftKeyInputFromExit(CallbackInfo ci) {
        // don't question the devs i guess? idk why this is needed
        if (this.wantsToStopRiding() && this.isPassenger())
            this.input.shiftKeyDown = false;
    }
}
