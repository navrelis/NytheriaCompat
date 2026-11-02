/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.IdMapper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.registries.GameData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.world.level.block.BlockInjection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(Block.class)
public abstract class BlockInject implements BlockInjection {
    @Shadow @Final @Mutable public static IdMapper<BlockState> BLOCK_STATE_REGISTRY;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void nytheriacompat$useBlockStateIdMap(CallbackInfo ci) {
        BLOCK_STATE_REGISTRY = GameData.getBlockStateIDMap();
    }

    @WrapOperation(method = "shouldRenderFace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;skipRendering(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
    private static boolean nytheriacompat$checkSupportsExternalHiding(BlockState instance, BlockState blockState, Direction direction, Operation<Boolean> original, @Local(argsOnly = true) BlockGetter level, @Local(argsOnly = true, ordinal = 1) BlockPos pos) {
        return original.call(instance, blockState, direction) || (blockState.hidesNeighborFace(level, pos, instance, direction.getOpposite()) && instance.supportsExternalFaceHiding());
    }

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;"))
    private static void nytheriacompat$beginCapturingDrops(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        nytheriacompat$beginCapturingDrops();
    }

    @WrapOperation(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V"))
    private static void nytheriacompat$handleBlockDrops(BlockState instance, ServerLevel level, BlockPos pos, ItemStack stack, boolean b, Operation<Void> original) {
        var captured = nytheriacompat$stopCapturingDrops();
        CommonHooks.nytheriacompat$handleBlockDrops(level, pos, instance, null, captured, null, ItemStack.EMPTY, () -> original.call(instance, level, pos, stack, b)); // Nytheria Compat TODO: do we need to pass false?
    }

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;"))
    private static void nytheriacompat$beginCapturingDrops(BlockState state, LevelAccessor level, BlockPos pos, BlockEntity blockEntity, CallbackInfo ci) {
        nytheriacompat$beginCapturingDrops();
    }

    @WrapOperation(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V"))
    private static void nytheriacompat$handleBlockDrops(BlockState instance, ServerLevel level, BlockPos pos, ItemStack stack, boolean b, Operation<Void> original, @Local(argsOnly = true) BlockEntity blockEntity) {
        var captured = nytheriacompat$stopCapturingDrops();
        CommonHooks.nytheriacompat$handleBlockDrops(level, pos, instance, blockEntity, captured, null, ItemStack.EMPTY, () -> original.call(instance, level, pos, stack, b)); // Nytheria Compat TODO: do we need to pass false?
    }

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"))
    private static void nytheriacompat$beginCapturingDrops(BlockState state, Level level, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemStack tool, CallbackInfo ci) {
        nytheriacompat$beginCapturingDrops();
    }

    @WrapOperation(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V"))
    private static void nytheriacompat$handleBlockDrops(BlockState instance, ServerLevel level, BlockPos pos, ItemStack stack, boolean b, Operation<Void> original, @Local(argsOnly = true) BlockEntity blockEntity, @Local(argsOnly = true) Entity entity) {
        var captured = nytheriacompat$stopCapturingDrops();
        CommonHooks.nytheriacompat$handleBlockDrops(level, pos, instance, blockEntity, captured, entity, ItemStack.EMPTY, () -> original.call(instance, level, pos, stack, b)); // Nytheria Compat TODO: do we need to pass false?
    }

    @ModifyExpressionValue(method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private static boolean nytheriacompat$checkIsRestoringBlockSnapshots(boolean original, @Local(argsOnly = true) Level level) {
        return original && !level.nytheriacompat$getRestoringBlockSnapshots();
    }

    @WrapOperation(method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean nytheriacompat$tryCaptureResourceDrops(Level instance, Entity entity, Operation<Boolean> original) {
        if (capturedDrops != null && entity instanceof ItemEntity itemEntity) {
            capturedDrops.put(itemEntity, () -> original.call(instance, entity));
            // Nytheria Compat: captured drops are spawned later by CommonHooks.nytheriacompat$handleBlockDrops, like NeoForge's patch
            return true;
        }

        return original.call(instance, entity);
    }

    @ModifyExpressionValue(method = "popExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean nytheriacompat$checkIsRestoringBlockSnapshots(boolean original, @Local(argsOnly = true) ServerLevel level) {
        return original && !level.nytheriacompat$getRestoringBlockSnapshots();
    }

    // Nytheria Compat: We're using a map instead, mod compatibility time.
    @Unique @Nullable
    private static Map<ItemEntity, Runnable> capturedDrops = null;

    @Unique
    private static void nytheriacompat$beginCapturingDrops() {
        capturedDrops = new LinkedHashMap<>();
    }

    @Unique
    private static Map<ItemEntity, Runnable> nytheriacompat$stopCapturingDrops() {
        Map<ItemEntity, Runnable> drops = capturedDrops;
        capturedDrops = null;
        return drops;
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
    }
}
