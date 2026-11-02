// TRACKED HASH: 7ec032f8735b23aa563858eb4a3555caa9c5d7ff
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.helpers.mixin.Extends;
import dev.nytheria.compat.injections.world.level.LevelInjection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

@Mixin(value = Level.class, priority = 1111) // higher priority to mixin to Porting Lib
@Extends(AttachmentHolder.class)
public abstract class LevelInject implements LevelAccessor, ILevelExtension, LevelInjection {
    public boolean restoringBlockSnapshots = false;
    public boolean captureBlockSnapshots = false;

    @Unique private final ArrayList<BlockEntity> freshBlockEntities = new ArrayList<>();
    @Unique private final ArrayList<BlockEntity> pendingFreshBlockEntities = new ArrayList<>();

    @Shadow @Final public boolean isClientSide;
    @Shadow public abstract ResourceKey<Level> dimension();
    @Shadow public abstract BlockState getBlockState(BlockPos blockPos);
    @Shadow public abstract void updateNeighbourForOutputSignal(BlockPos pos, Block block);
    @Shadow @Final private ResourceKey<Level> dimension;

    @Shadow
    private boolean tickingBlockEntities;
    public ArrayList<BlockSnapshot> capturedBlockSnapshots = new ArrayList<>();

    @Override
    public ArrayList<BlockSnapshot> nytheriacompat$getCapturedBlockSnapshots() {
        return capturedBlockSnapshots;
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;", ordinal = 0, shift = At.Shift.AFTER))
    private void nytheriacompat$captureSnapshot(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) LocalRef<BlockPos> posRef, @Share("blockSnapshot") LocalRef<BlockSnapshot> blockSnapshot) {
        // Nytheria Compat: the snapshot needs an immutable position; without a capture the caller's position stays as it is
        if (this.captureBlockSnapshots && !this.isClientSide) {
            posRef.set(pos.immutable());
            blockSnapshot.set(BlockSnapshot.create(this.dimension, (Level) (Object) this, posRef.get()));
            this.capturedBlockSnapshots.add(blockSnapshot.get());
        }
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "RETURN", ordinal = 2))
    private void nytheriacompat$removeCapturedSnapshot(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir, @Share("blockSnapshot") LocalRef<BlockSnapshot> blockSnapshot) {
        if (blockSnapshot.get() != null) {
            this.capturedBlockSnapshots.remove(blockSnapshot.get());
        }
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), cancellable = true)
    private void nytheriacompat$cancelIfCapturing(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir, @Share("blockSnapshot") LocalRef<BlockSnapshot> blockSnapshot) {
        if (blockSnapshot.get() != null) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public void addFreshBlockEntities(Collection<BlockEntity> list) {
        if (this.tickingBlockEntities) {
            this.pendingFreshBlockEntities.addAll(list);
        } else {
            this.freshBlockEntities.addAll(list);
        }
    }

    @Inject(method = "removeBlockEntity", at = @At("TAIL"))
    public void nytheriacompat$updateNeighbourOutputSignalsForRemoval(BlockPos pos, CallbackInfo ci) {
        this.updateNeighbourForOutputSignal(pos, this.getBlockState(pos).getBlock());
    }

    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("TAIL"))
    private void nytheriacompat$addPartEntitiesToList(Entity entity, AABB area, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir, @Local List<Entity> list) {
        for (PartEntity<?> partEntity : this.nytheriacompat$getPartEntities()) {
            if (partEntity != entity && partEntity.getBoundingBox().intersects(area) && predicate.test(partEntity)) {
                list.add(partEntity);
            }
        }
    }

    @Inject(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V", at = @At("TAIL"))
    private <T extends Entity> void nytheriacompat$addPartEntitiesToList(EntityTypeTest<Entity, T> entityTypeTest, AABB bounds, Predicate<? super T> predicate, List<? super T> output, int maxResults, CallbackInfo ci) {
        for (PartEntity<?> partEntity : this.nytheriacompat$getPartEntities()) {
            var castEntity = entityTypeTest.tryCast(partEntity);
            if (castEntity != null && partEntity.getBoundingBox().intersects(bounds) && predicate.test(castEntity)) {
                // TODO: Nytheria Compat: doesn't this technically overflow the maxResults?
                output.add(castEntity);
                if (output.size() >= maxResults)
                    break;
            }
        }
    }

    private double maxEntityRadius = 2.0D;

    @Override
    public double getMaxEntityRadius() {
        return maxEntityRadius;
    }

    @Override
    public double increaseMaxEntityRadius(double value) {
        if (value > maxEntityRadius)
            maxEntityRadius = value;

        return maxEntityRadius;
    }

    @ApiStatus.Internal @Override public abstract void setDayTimeFraction(float dayTimeFraction);
    @ApiStatus.Internal @Override public abstract float getDayTimeFraction();
    @Override public abstract float getDayTimePerTick();
    @Override public abstract void setDayTimePerTick(float dayTimePerTick);

    @Override
    public long advanceDaytime() {
        if (this.getDayTimePerTick() < 0) {
            return 1L;
        }

        float dayTimeStep = this.getDayTimeFraction() + this.getDayTimePerTick();
        long result = (long) dayTimeStep;
        this.setDayTimeFraction(dayTimeStep - result);

        return result;
    }

    // Nytheria Compat
    @Override
    public void nytheriacompat$setCapturingBlockSnapshots(boolean value) {
        this.captureBlockSnapshots = value;
    }

    @Override
    public void nytheriacompat$setRestoringBlockSnapshots(boolean value) {
        this.restoringBlockSnapshots = value;
    }

    @Override
    public boolean nytheriacompat$getCapturingBlockSnapshots() {
        return this.captureBlockSnapshots;
    }

    @Override
    public boolean nytheriacompat$getRestoringBlockSnapshots() {
        return this.restoringBlockSnapshots;
    }
}
