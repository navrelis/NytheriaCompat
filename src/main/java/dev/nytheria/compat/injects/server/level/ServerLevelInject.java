// TRACKED HASH: 853e9e4639ba453c25a048d0905ec758c41a51dd
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.level;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.CapabilityListenerHolder;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import net.neoforged.neoforge.common.IOUtilities;
import dev.nytheria.compat.hook.EventGate;
import dev.nytheria.compat.hook.Scope;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.server.level.ServerLevelInjection;

import java.util.Collection;
import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelInject extends Level implements ServerLevelInjection, ILevelExtension {
    protected ServerLevelInject(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Shadow @Final private ServerLevelData serverLevelData;
    @Shadow @Final private MinecraftServer server;

    @Unique final Int2ObjectMap<PartEntity<?>> nytheriacompat$entityParts = new Int2ObjectOpenHashMap<>();

    // Nytheria Compat: NeoForge's ServerLevel#syncData, sends changes of synced level attachments to the players in this level
    public final void syncData(AttachmentType<?> type) {
        AttachmentSync.syncLevelUpdate((ServerLevel) (Object) this, type);
    }

    // Nytheria Compat: asks the set itself, because Porting Lib's chunk_loading redirects this isEmpty() to hasForcedChunks, which inverts
    // it and drops the set; original is not called for that reason. The set has to stay: Kibe's chunk loaders keep a level ticking
    // by redirecting getForcedChunks() here. hasForcedChunks adds NeoForge's tickets. With or without either redirect a level
    // without players goes idle after 300 ticks only when nothing forces it, as in vanilla.
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;isEmpty()Z"))
    private boolean nytheriacompat$checkHasForcedChunks(LongSet instance, Operation<Boolean> original) {
        return instance.isEmpty() && !ForcedChunkManager.hasForcedChunks((ServerLevel) (Object) this);
    }

    @WrapOperation(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void nytheriacompat$callEntityTickEvents(Entity instance, Operation<Void> original) {
        if (!EventHooks.isEntityTickPreCanceled(instance)) {
            original.call(instance);
            EventHooks.fireEntityTickPost(instance);
        }
    }

    @Inject(method = "save", at = @At("TAIL"))
    private void nytheriacompat$handleSaveEvent(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (!skipSave) {
            if (EventGate.has(LevelEvent.Save.class)) NeoForge.EVENT_BUS.post(new LevelEvent.Save(this));

            if (flush) {
                IOUtilities.waitUntilIOWorkerComplete(); // Nytheria Compat TODO: is this needed?
            }
        }
    }

    @Inject(method = "addPlayer", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$callEntityJoinLevelEvent(ServerPlayer player, CallbackInfo ci) {
        if (EventGate.has(EntityJoinLevelEvent.class) && NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(player, this)).isCanceled()) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "addPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addNewEntity(Lnet/minecraft/world/level/entity/EntityAccess;)Z"))
    private <T extends EntityAccess> boolean nytheriacompat$markAddWithoutEvent(PersistentEntitySectionManager<T> instance, T entityAccess, Operation<Boolean> original) {
        instance.nytheriacompat$markWithoutEvent();
        return original.call(instance, entityAccess);
    }

    @Inject(method = "addPlayer", at = @At("TAIL"))
    private void nytheriacompat$addEntityToWorld(ServerPlayer player, CallbackInfo ci) {
        player.onAddedToLevel();
    }

    @WrapOperation(method = "addEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addNewEntity(Lnet/minecraft/world/level/entity/EntityAccess;)Z"))
    private <T extends EntityAccess> boolean nytheriacompat$checkEntityAddToWorld(PersistentEntitySectionManager<T> instance, T entity, Operation<Boolean> original) {
        //noinspection MixinExtrasOperationParameters
        if (original.call(instance, entity)) {
            ((Entity) entity).onAddedToLevel();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Int2ObjectMap<PartEntity<?>> nytheriacompat$getEntityParts() {
        return this.nytheriacompat$entityParts;
    }

    @Override
    public Collection<PartEntity<?>> nytheriacompat$getPartEntities() {
        return this.nytheriacompat$entityParts.values();
    }

    @Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
    public static abstract class EntityCallbacksInject implements LevelCallback<Entity> {
        @Shadow @Final
        ServerLevel field_26936;

        @Definition(id = "entity", local = @Local(type = Entity.class, argsOnly = true))
        @Definition(id = "EnderDragon", type = EnderDragon.class)
        @Expression("entity instanceof EnderDragon")
        @WrapOperation(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("MIXINEXTRAS:EXPRESSION"))
        private boolean nytheriacompat$startTrackingMultipart(Object object, Operation<Boolean> original) {
            if (original.call(object))
                return true;

            if (Scope.isNeoObject((Entity) object) && ((Entity) object).isMultipartEntity()) {
                var parts = ((Entity) object).getParts();

                if (parts != null) {
                    for (PartEntity<?> part : parts) {
                        field_26936.nytheriacompat$getEntityParts().put(part.getId(), part);
                    }
                }
            }

            return false;
        }

        @Definition(id = "entity", local = @Local(type = Entity.class, argsOnly = true))
        @Definition(id = "EnderDragon", type = EnderDragon.class)
        @Expression("entity instanceof EnderDragon")
        @WrapOperation(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("MIXINEXTRAS:EXPRESSION"))
        private boolean nytheriacompat$stopTrackingMultipart(Object object, Operation<Boolean> original) {
            if (original.call(object))
                return true;

            if (Scope.isNeoObject((Entity) object) && ((Entity) object).isMultipartEntity()) {
                var parts = ((Entity) object).getParts();

                if (parts != null) {
                    for (PartEntity<?> part : parts) {
                        field_26936.nytheriacompat$getEntityParts().remove(part.getId());
                    }
                }
            }

            return false;
        }

        @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
        private void nytheriacompat$callEntityLevelRemoveEvent(Entity entity, CallbackInfo ci) {
            entity.onRemovedFromLevel();
        }
    }

    @Unique private final CapabilityListenerHolder capListenerHolder = new CapabilityListenerHolder();

    @Override
    public void invalidateCapabilities(BlockPos pos) {
        this.capListenerHolder.invalidatePos(pos);
    }

    @Override
    public void invalidateCapabilities(ChunkPos pos) {
        this.capListenerHolder.invalidateChunk(pos);
    }

    @Override
    public void registerCapabilityListener(BlockPos pos, ICapabilityInvalidationListener listener) {
        this.capListenerHolder.addListener(pos, listener);
    }

    @Override
    public void cleanCapabilityListenerReferences() {
        this.capListenerHolder.clean();
    }

    @Override
    public void setDayTimeFraction(float dayTimeFraction) {
        this.serverLevelData.setDayTimeFraction(dayTimeFraction);
    }

    @Override
    public float getDayTimeFraction() {
        return this.serverLevelData.getDayTimeFraction();
    }

    @Override
    public float getDayTimePerTick() {
        return this.serverLevelData.getDayTimePerTick();
    }

    @Override
    public void setDayTimePerTick(float dayTimePerTick) {
        if (dayTimePerTick != this.getDayTimePerTick() && dayTimePerTick != 0f) {
            this.serverLevelData.setDayTimePerTick(dayTimePerTick);
            this.server.forceTimeSynchronization();
        }
    }
}