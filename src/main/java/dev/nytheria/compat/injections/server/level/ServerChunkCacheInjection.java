/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.server.level;

import io.github.fabricators_of_create.porting_lib.chunk.loading.extensions.ServerChunkCacheExtension;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

public interface ServerChunkCacheInjection extends ServerChunkCacheExtension {
    default <T> void addRegionTicket(TicketType<T> ticketType, ChunkPos chunkPos, int i, T object, boolean forceTicks) {
        throw new IllegalStateException();
    }

    default <T> void removeRegionTicket(TicketType<T> ticketType, ChunkPos chunkPos, int i, T object, boolean forceTicks) {
        throw new IllegalStateException();
    }
}
