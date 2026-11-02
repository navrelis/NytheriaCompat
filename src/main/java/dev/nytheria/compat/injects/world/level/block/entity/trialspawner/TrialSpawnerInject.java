/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.block.entity.trialspawner;

import com.mojang.datafixers.util.Either;
import net.neoforged.neoforge.common.extensions.IOwnedSpawner;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;

@Mixin(TrialSpawner.class)
public abstract class TrialSpawnerInject implements IOwnedSpawner {
    @Shadow @Final private TrialSpawner.StateAccessor stateAccessor;

    @Override
    public @Nullable Either<BlockEntity, Entity> getOwner() {
        if (this.stateAccessor instanceof TrialSpawnerBlockEntity blockEntity) {
            return Either.left(blockEntity);
        }

        return null;
    }
}
