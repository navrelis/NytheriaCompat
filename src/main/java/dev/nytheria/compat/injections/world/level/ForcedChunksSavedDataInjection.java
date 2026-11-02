/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level;

import io.github.fabricators_of_create.porting_lib.chunk.loading.extensions.ForcedChunksSavedDataExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.UUID;

@FabricInjectedInterface(ForcedChunksSavedData.class)
public interface ForcedChunksSavedDataInjection extends ForcedChunksSavedDataExtension {
    default ForcedChunkManager.TicketTracker<BlockPos> neo$getBlockForcedChunks() {
        throw NytheriaCompatHelper.createMixinException(ForcedChunksSavedDataInjection.class, "neo$getBlockForcedChunks");
    }

    default ForcedChunkManager.TicketTracker<UUID> neo$getEntityForcedChunks() {
        throw NytheriaCompatHelper.createMixinException(ForcedChunksSavedDataInjection.class, "neo$getEntityForcedChunks");
    }
}
