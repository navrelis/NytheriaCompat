// TRACKED HASH: f83737c1722cf4974da22578c25c45ca640818c9
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.injections.world.level.ForcedChunksSavedDataInjection;

import java.util.UUID;

@Mixin(ForcedChunksSavedData.class)
public abstract class ForcedChunksSavedDataInject implements ForcedChunksSavedDataInjection {

    @Unique private final ForcedChunkManager.TicketTracker<BlockPos> neo$blockForcedChunks = new ForcedChunkManager.TicketTracker<>();
    @Unique private final ForcedChunkManager.TicketTracker<UUID> neo$entityForcedChunks = new ForcedChunkManager.TicketTracker<>();

    @ModifyReturnValue(method = "load", at = @At("RETURN"))
    private static ForcedChunksSavedData nytheriacompat$readModForcedChunks(ForcedChunksSavedData original, @Local(argsOnly = true) CompoundTag tag) {
        ForcedChunkManager.readModForcedChunks(tag, original.neo$getBlockForcedChunks(), original.neo$getEntityForcedChunks());
        return original;
    }

    @ModifyReturnValue(method = "save", at = @At("RETURN"))
    private CompoundTag nytheriacompat$writeModForcedChunks(CompoundTag original) {
        ForcedChunkManager.writeModForcedChunks(original, this.neo$blockForcedChunks, this.neo$entityForcedChunks);
        return original;
    }

    @Override
    public ForcedChunkManager.TicketTracker<BlockPos> neo$getBlockForcedChunks() {
        return this.neo$blockForcedChunks;
    }

    @Override
    public ForcedChunkManager.TicketTracker<UUID> neo$getEntityForcedChunks() {
        return this.neo$entityForcedChunks;
    }
}