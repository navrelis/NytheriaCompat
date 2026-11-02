// TRACKED HASH: 9493f81a6485a3765611155c032cf421d0ceeaf2
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.multiplayer;

import dev.nytheria.compat.hook.EventGate;
import dev.nytheria.compat.hook.Scope;
import java.util.Collection;
import java.util.function.Supplier;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.neoforged.neoforge.client.model.data.ModelDataManager;
import net.neoforged.neoforge.client.model.lighting.QuadLighter;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.client.multiplayer.ClientLevelInjection;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;

@Mixin(ClientLevel.class)
public abstract class ClientLevelInject extends Level implements ClientLevelInjection, ILevelExtension {
    @Unique private final ModelDataManager modelDataManager = new ModelDataManager((ClientLevel) (Object) this);
    @Unique private final Int2ObjectMap<PartEntity<?>> nytheriacompat$partEntities = new Int2ObjectOpenHashMap<>();

    protected ClientLevelInject(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Shadow public abstract DimensionSpecialEffects effects();

    @Inject(method = "setBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void nytheriacompat$storeCurentBlockSnapshot(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir, @Share("snapshot") LocalRef<BlockSnapshot> snapshotRef) {
        snapshotRef.set(BlockSnapshot.create(this.dimension(), this, pos, flags));
    }

    @WrapWithCondition(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    public boolean nytheriacompat$onlyTickIfCanUpdate(Entity entity) {
        return !EventHooks.isEntityTickPreCanceled(entity);
    }

    @Inject(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V", shift = At.Shift.AFTER))
    private void nytheriacompat$postEntityTickEvent(Entity entity, CallbackInfo ci) {
        EventHooks.fireEntityTickPost(entity);
    }

    @Inject(at = @At("HEAD"), method = "addEntity", cancellable = true)
    public void nytheriacompat$runJoinLevelEvent(Entity entity, CallbackInfo ci) {
        if (EventGate.has(EntityJoinLevelEvent.class) && NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(entity, (ClientLevel) (Object) this)).isCanceled())
            ci.cancel();
    }

    @Mixin(targets = "net/minecraft/client/multiplayer/ClientLevel$EntityCallbacks")
    public abstract static class EntityCallbacksInject {
        @Shadow @Final private ClientLevel field_27735;

        @Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
        private void nytheriacompat$addForgeMultipartEntitiesToLevel(Entity entity, CallbackInfo ci) {
            if (Scope.isNeoObject(entity) && entity.isMultipartEntity() && entity.getParts() != null) {
                for (PartEntity<?> part : entity.getParts()) {
                    field_27735.nytheriacompat$getPartEntitiesMap().put(part.getId(), part);
                }
            }
        }

        @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
        private void nytheriacompat$removeForgeMultipartEntitiesFromLevel(Entity entity, CallbackInfo ci) {
            entity.onRemovedFromLevel();

            if (Scope.isNeoObject(entity) && entity.isMultipartEntity() && entity.getParts() != null) {
                for (PartEntity<?> part : entity.getParts()) {
                    field_27735.nytheriacompat$getPartEntitiesMap().remove(part.getId());
                }
            }
        }
    }

    @Override
    public Collection<PartEntity<?>> nytheriacompat$getPartEntities() {
        return this.nytheriacompat$partEntities.values();
    }

    @Override
    public ModelDataManager getModelDataManager() {
        return modelDataManager;
    }

    @Unique
    public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
        var constantAmbientLight = this.effects().constantAmbientLight();

        if (!shade)
            return constantAmbientLight ? 0.9F : 1F;

        return QuadLighter.calculateShade(normalX, normalY, normalZ, constantAmbientLight);
    }

    @Override
    public Int2ObjectMap<PartEntity<?>> nytheriacompat$getPartEntitiesMap() {
        return this.nytheriacompat$partEntities;
    }

    // Variable day time code :,D
    private float dayTimeFraction = 0.0f;
    private float dayTimePerTick = -1.0f;

    @Override
    public void setDayTimeFraction(float dayTimeFraction) {
        this.dayTimeFraction = dayTimeFraction;
    }

    @Override
    public float getDayTimeFraction() {
        return dayTimeFraction;
    }

    @Override
    public float getDayTimePerTick() {
        return dayTimePerTick;
    }

    @Override
    public void setDayTimePerTick(float dayTimePerTick) {
        this.dayTimePerTick = dayTimePerTick;
    }
}
