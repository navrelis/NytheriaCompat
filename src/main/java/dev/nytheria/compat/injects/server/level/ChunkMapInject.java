// TRACKED HASH: cebcc0747792b8bfe53d24573c9609f5c22b61d1
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.server.level.ChunkMapInjection;
import dev.nytheria.compat.mixin.TrackedEntityAccessor;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.ChunkAccess;

@Mixin(value = ChunkMap.class, priority = 1050)
public abstract class ChunkMapInject implements ChunkMapInjection {
    @Shadow @Final private ServerLevel level;


    @Shadow
    @Final
    private ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> mainThreadMailbox;

    @Shadow
    @Final
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Inject(method = "method_60440", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;setLoaded(Z)V", ordinal = 0, shift = At.Shift.AFTER))
    private void nytheriacompat$callChunkUnloadEvent(ChunkHolder chunkHolder, long l, CallbackInfo ci, @Local ChunkAccess chunkAccess) {
        if (EventGate.has(ChunkEvent.Unload.class)) NeoForge.EVENT_BUS.post(new ChunkEvent.Unload(chunkAccess));
    }

    @Override
    public List<ServerPlayer> getPlayersWatching(Entity entity) {
        var trackedEntity = this.entityMap.get(entity.getId());

        if (trackedEntity != null) {
            var ret = new ArrayList<ServerPlayer>(((TrackedEntityAccessor) trackedEntity).getSeenBy().size());
            for (ServerPlayerConnection connection : ((TrackedEntityAccessor) trackedEntity).getSeenBy()) {
                ret.add(connection.getPlayer());
            }

            return Collections.unmodifiableList(ret);
        }

        return List.of();
    }

    public void scheduleOnMainThreadMailbox(ChunkTaskPriorityQueueSorter.Message<Runnable> msg) {
        this.mainThreadMailbox.tell(msg);
    }
}
