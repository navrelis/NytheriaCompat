/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.multiplayer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeInject {
    @Shadow @Final private Minecraft minecraft;

    // Nytheria Compat: asked against IBlockExtension, which declares onDestroyedByPlayer and getSoundType as defaults whose bodies are what they replace; against Block.class both were true for every block.
    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", ordinal = 0))
    private boolean nytheriacompat$callDestroyedByPlayer(Level instance, BlockPos pos, BlockState newState, int flags, Operation<Boolean> original, @Local BlockState state, @Local FluidState fluidState) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(state.getBlock().getClass(), IBlockExtension.class, "onDestroyedByPlayer", BlockState.class, Level.class, BlockPos.class, Player.class, boolean.class, FluidState.class)) {
            return state.onDestroyedByPlayer(instance, pos, minecraft.player, false, fluidState);
        }

        return original.call(instance, pos, newState, flags);
    }

    @WrapOperation(method = "method_41936", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean nytheriacompat$handleLeftClickBlock(MultiPlayerGameMode instance, BlockPos pos, Operation<Boolean> original, @Local(argsOnly = true) Direction direction) {
        if (!CommonHooks.onLeftClickBlock(minecraft.player, pos, direction, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK).isCanceled()) {
            return original.call(instance, pos);
        } else {
            return false;
        }
    }

    @Unique private PlayerInteractEvent.LeftClickBlock nytheriacompat$leftClickBlock;

    @Inject(method = "startDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", ordinal = 1))
    private void nytheriacompat$callForgeLeftClickBlockEvent(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
        this.nytheriacompat$leftClickBlock = CommonHooks.onLeftClickBlock(this.minecraft.player, loc, face, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
    }

    @WrapOperation(method = "method_41935", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean nytheriacompat$checkCanDestroyBlock(MultiPlayerGameMode instance, BlockPos pos, Operation<Boolean> original, @Local(argsOnly = true) Direction direction) {
        if (!CommonHooks.onLeftClickBlock(this.minecraft.player, pos, direction, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK).isCanceled()) {
            return original.call(instance, pos);
        } else {
            return false;
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/Tutorial;onDestroyBlock(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;F)V", ordinal = 1), cancellable = true)
    private void nytheriacompat$checkClientMineHold(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
        if (CommonHooks.onClientMineHold(this.minecraft.player, posBlock, directionFacing).getUseItem() == TriState.FALSE)
            cir.setReturnValue(true);
    }

    // Nytheria Compat: block reach handled by other mods

    @Inject(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void nytheriacompat$cancelRightClickEvent(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir, @Local BlockPos pos, @Share("event") LocalRef<PlayerInteractEvent.RightClickBlock> eventRef) {
        var event = CommonHooks.onRightClickBlock(player, hand, pos, result);
        eventRef.set(event);

        if (event.isCanceled())
            cir.setReturnValue(event.getCancellationResult());
    }

    // Nytheria Compat: handled via Architectury
    @Inject(method = "method_41929", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;"), cancellable = true)
    private void nytheriacompat$callItemRightClickEvent(InteractionHand interactionHand, Player player, MutableObject<InteractionResult> mutableObject, int i, CallbackInfoReturnable<Packet> cir, @Local ServerboundUseItemPacket packet) {
        var cancelResult = CommonHooks.onItemRightClick(player, interactionHand);

        if (cancelResult != null) {
            mutableObject.setValue(cancelResult);
            cir.setReturnValue(packet);
        }
    }

    // Nytheria Compat: NeoForge's EntityInteractSpecific is bridged from Porting Lib (NytheriaCompat.kt), which fires it before
    //  Entity.interactAt like NeoForge. The TAIL inject here fired it a second time, after interactAt had already run.
}
