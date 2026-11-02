// TRACKED HASH: 8167cd6a6327883a03de56b778f6c1f9c9fe8583
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.commands.arguments.coordinates;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockPosArgument.class)
public abstract class BlockPosArgumentInject {
    @WrapOperation(method = "getLoadedBlockPos(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/server/level/ServerLevel;Ljava/lang/String;)Lnet/minecraft/core/BlockPos;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;hasChunkAt(Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean nytheriacompat$checkChunkUsingUnsidedLevel(ServerLevel instance, BlockPos blockPos, Operation<Boolean> original, @Local(argsOnly = true) CommandContext<CommandSourceStack> ctx) {
        if (instance == null) {
            return ctx.getSource().getUnsidedLevel().hasChunkAt(blockPos);
        }

        return original.call(instance, blockPos) || ctx.getSource().getUnsidedLevel().hasChunkAt(blockPos);
    }

    @WrapOperation(method = "getLoadedBlockPos(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/server/level/ServerLevel;Ljava/lang/String;)Lnet/minecraft/core/BlockPos;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isInWorldBounds(Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean nytheriacompat$checkBoundsUsingUnsidedLevel(ServerLevel instance, BlockPos blockPos, Operation<Boolean> original, @Local(argsOnly = true) CommandContext<CommandSourceStack> ctx) {
        if (instance == null) {
            return ctx.getSource().getUnsidedLevel().isInWorldBounds(blockPos);
        }

        return original.call(instance, blockPos) || ctx.getSource().getUnsidedLevel().isInWorldBounds(blockPos);
    }
}