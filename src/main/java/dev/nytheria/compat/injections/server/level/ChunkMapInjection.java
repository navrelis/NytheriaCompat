/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.server.level;

import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.List;

public interface ChunkMapInjection {
    default void scheduleOnMainThreadMailbox(ChunkTaskPriorityQueueSorter.Message<Runnable> msg) {
        throw NytheriaCompatHelper.createMixinException(ChunkMapInjection.class, "scheduleOnMainThreadMailbox");
    }

    default List<ServerPlayer> getPlayersWatching(Entity entity) {
        throw NytheriaCompatHelper.createMixinException(ChunkMapInjection.class, "getPlayersWatching");
    }
}
