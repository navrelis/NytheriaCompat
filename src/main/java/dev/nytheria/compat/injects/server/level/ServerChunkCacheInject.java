/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.level;

import dev.nytheria.compat.injections.server.level.ServerChunkCacheInjection;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheInject implements ServerChunkCacheInjection {
    @Shadow
    public abstract <T> void addRegionTicket(TicketType<T> type, ChunkPos pos, int distance, T value);

    @Shadow
    public abstract <T> void removeRegionTicket(TicketType<T> type, ChunkPos pos, int distance, T value);

    // Nytheria Compat: vanilla has no per-ticket forceTicks, so the ticket is the plain vanilla region ticket (forced chunks load and tick like /forceload)
    @Override
    public <T> void addRegionTicket(TicketType<T> ticketType, ChunkPos chunkPos, int i, T object, boolean forceTicks) {
        this.addRegionTicket(ticketType, chunkPos, i, object);
    }

    @Override
    public <T> void removeRegionTicket(TicketType<T> ticketType, ChunkPos chunkPos, int i, T object, boolean forceTicks) {
        this.removeRegionTicket(ticketType, chunkPos, i, object);
    }
}
