// TRACKED HASH: 6b790991592a47c6c6a8e8be25116c80e58fd91d
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.level;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeInject {
	@Shadow private GameType gameModeForPlayer;
	@Shadow @Final protected ServerPlayer player;
	@Shadow protected ServerLevel level;

	// Nytheria Compat: onLeftClickBlock handled by Porting Lib

	@WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;canAttackBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z"))
	private boolean nytheriacompat$handleFireBlockBreakEvent(Item instance, BlockState state, Level level, BlockPos pos, Player player, Operation<Boolean> original) {
		if (player instanceof ServerPlayer serverPlayer) {
			var event = CommonHooks.nytheriacompat$fireBlockBreak(level, this.gameModeForPlayer, serverPlayer, pos, state, stack -> original.call(instance, state, level, pos, player));
			return !event.isCanceled();
		}

		return original.call(instance, state, level, pos, player);
	}

	// Nytheria Compat: asked against IBlockExtension, which declares onDestroyedByPlayer (with its Level parameter) as a default whose server body is the removeBlock call it replaces; the old signature had no Level, so it never matched.
	@WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
	private boolean nytheriacompat$tryHandleDestroyedByPlayer(ServerLevel instance, BlockPos blockPos, boolean b, Operation<Boolean> original, @Local(ordinal = 1) BlockState state) {
		if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(state.getBlock().getClass(), IBlockExtension.class, "onDestroyedByPlayer", BlockState.class, Level.class, BlockPos.class, Player.class, boolean.class, FluidState.class)) {
			return state.onDestroyedByPlayer(instance, blockPos, this.player, b, this.level.getFluidState(blockPos));
		}

		return original.call(instance, blockPos, b);
	}

	// Nytheria Compat: onItemRightClick handled by Porting Lib

	@Definition(id = "gameModeForPlayer", field = "Lnet/minecraft/server/level/ServerPlayerGameMode;gameModeForPlayer:Lnet/minecraft/world/level/GameType;")
	@Definition(id = "SPECTATOR", field = "Lnet/minecraft/world/level/GameType;SPECTATOR:Lnet/minecraft/world/level/GameType;")
	@Expression("this.gameModeForPlayer == SPECTATOR")
	@Inject(method = "useItemOn", at = @At("MIXINEXTRAS:EXPRESSION"), cancellable = true)
	private void nytheriacompat$cancelRightClickEvent(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir, @Local BlockPos pos, @Share("event") LocalRef<PlayerInteractEvent.RightClickBlock> eventRef) {
		var event = CommonHooks.onRightClickBlock(player, hand, pos, result);
		eventRef.set(event);

		if (event.isCanceled())
			cir.setReturnValue(event.getCancellationResult());
	}
}
