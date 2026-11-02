/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Reference2BooleanLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Intrinsic;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.injections.world.level.block.FireBlockInjection;
import dev.nytheria.compat.hook.Scope;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@Mixin(FireBlock.class)
public abstract class FireBlockInject extends BaseFireBlock implements FireBlockInjection {
    @Shadow protected abstract void checkBurnOut(Level level, BlockPos pos, int chance, RandomSource random, int age);

    private final ThreadLocal<Direction> nytheriacompat$face = new ThreadLocal<>();
    // Nytheria Compat: the checks below ask IBlockExtension, which declares getFlammability, onCaughtFire and getFireSpreadSpeed as defaults (vanilla's burn odds / ignite odds, and a no-op that
    //  leaves TNT to TntBlockInject); against Block.class they were true for every block.
    @Unique private static final Reference2BooleanLinkedOpenHashMap<Class<? extends Block>> nytheriacompat$cachedFlammabilityOverride = new Reference2BooleanLinkedOpenHashMap<>();

    public FireBlockInject(Properties properties, float fireDamage) {
        super(properties, fireDamage);
    }

    @WrapOperation(method = "getStateForPlacement(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;canBurn(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 0))
    private boolean nytheriacompat$checkCanCatchFire(FireBlock instance, BlockState state, Operation<Boolean> original, @Local(argsOnly = true) BlockGetter level, @Local(argsOnly = true) BlockPos pos) {
        return original.call(instance, state) || (Scope.isNeoObject((Object) state.getBlock()) && instance.canCatchFire(level, pos, Direction.UP));
    }

    @WrapOperation(method = "getStateForPlacement(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;canBurn(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 1))
    private boolean nytheriacompat$checkCanCatchFire(FireBlock instance, BlockState state, Operation<Boolean> original, @Local(argsOnly = true) BlockGetter level, @Local(argsOnly = true) BlockPos pos, @Local Direction direction) {
        return original.call(instance, state) || (Scope.isNeoObject((Object) state.getBlock()) && instance.canCatchFire(level, pos.relative(direction), direction.getOpposite()));
    }

    @WrapOperation(method = "isValidFireLocation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;canBurn(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean nytheriacompat$checkCanCatchFireValidLocation(FireBlock instance, BlockState state, Operation<Boolean> original, @Local(argsOnly = true) BlockGetter level, @Local(argsOnly = true) BlockPos pos, @Local Direction direction) {
        return original.call(instance, state) || (Scope.isNeoObject((Object) state.getBlock()) && instance.canCatchFire(level, pos.relative(direction), direction.getOpposite()));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;canBurn(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 0))
    private boolean nytheriacompat$checkCanCatchFireTick(FireBlock instance, BlockState state, Operation<Boolean> original, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos pos) {
        return original.call(instance, state) || (Scope.isNeoObject((Object) state.getBlock()) && instance.canCatchFire(level, pos.below(), Direction.UP));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;checkBurnOut(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/util/RandomSource;I)V"))
    private void nytheriacompat$setDirection(FireBlock instance, Level level, BlockPos pos, int chance, RandomSource random, int age, Operation<Void> original, @Local(argsOnly = true) BlockPos originalPos) {
        var relativeX = pos.getX() - originalPos.getX();
        var relativeY = pos.getY() - originalPos.getY();
        var relativeZ = pos.getZ() - originalPos.getZ();

        var direction = Direction.fromDelta(relativeX, relativeY, relativeZ);
        if (direction != null)
            nytheriacompat$face.set(direction.getOpposite());
        original.call(instance, level, pos, chance, random, age);
        nytheriacompat$face.remove();
    }

    @Intrinsic
    private void tryCatchFire(Level level, BlockPos pos, int chance, RandomSource random, int age, Direction face) {
        nytheriacompat$face.set(face);
        this.checkBurnOut(level, pos, chance, random, age);
        nytheriacompat$face.remove();
    }

    @WrapOperation(method = "checkBurnOut", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;getBurnOdds(Lnet/minecraft/world/level/block/state/BlockState;)I"))
    private int nytheriacompat$checkFlammability(FireBlock instance, BlockState state, Operation<Integer> original, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos) {
        if (nytheriacompat$cachedFlammabilityOverride.computeIfAbsent(state.getBlock().getClass(), $ -> NytheriaCompatHelper.INSTANCE.hasMethodOverride(state.getBlock().getClass(), IBlockExtension.class, "getFlammability", BlockState.class, BlockGetter.class, BlockPos.class, Direction.class))) {
            return state.getFlammability(level, pos, nytheriacompat$face.get());
        }

        return original.call(instance, state);
    }

    @WrapOperation(method = "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;getIgniteOdds(Lnet/minecraft/world/level/block/state/BlockState;)I"))
    private int nytheriacompat$tryCheckFireSpreadSpeed(FireBlock instance, BlockState state, Operation<Integer> original, @Local(argsOnly = true) LevelReader level, @Local(argsOnly = true) BlockPos pos, @Local Direction direction) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(state.getBlock().getClass(), IBlockExtension.class, "getFireSpreadSpeed", BlockState.class, BlockGetter.class, BlockPos.class, Direction.class)) {
            return state.getFireSpreadSpeed(level, pos.relative(direction), direction.getOpposite());
        }

        return original.call(instance, state);
    }

    @Override
    public boolean canCatchFire(BlockGetter level, BlockPos pos, Direction face) {
        return level.getBlockState(pos).isFlammable(level, pos, face);
    }
}