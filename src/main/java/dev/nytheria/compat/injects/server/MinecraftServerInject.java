// TRACKED HASH: 0a24b5b7aafa1b5d82426467010c877512d3e484
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.network.payload.ClientboundCustomSetTimePayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.server.MinecraftServerInjection;
import dev.nytheria.compat.injections.world.item.alchemy.PotionBrewingInjection;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerInject implements MinecraftServerInjection {
    @Shadow private MinecraftServer.ReloadableResources resources;
    @Shadow public abstract RegistryAccess.Frozen registryAccess();
    @Shadow public abstract PlayerList getPlayerList();

    @WrapOperation(method = "spin", at = @At(value = "NEW", target = "java/lang/Thread"))
    private static Thread nytheriacompat$setThreadGroup(Runnable target, String name, Operation<Thread> original) {
        return new Thread(SidedThreadGroups.SERVER, target, name);
    }

    @ModifyExpressionValue(method = "prepareLevels", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongIterator;hasNext()Z"))
    private boolean nytheriacompat$tryReinstatePersistentChunks(boolean original, @Local(ordinal = 1) ServerLevel level, @Local ForcedChunksSavedData savedData) {
        if (!original) {
            ForcedChunkManager.reinstatePersistentChunks(level, savedData);
        }

        return original;
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/alchemy/PotionBrewing;bootstrap(Lnet/minecraft/world/flag/FeatureFlagSet;)Lnet/minecraft/world/item/alchemy/PotionBrewing;"))
    private PotionBrewing nytheriacompat$addRegistryAccessToBrewingBootstrap(FeatureFlagSet enabledFeatures, Operation<PotionBrewing> original) {
        try {
            PotionBrewingInjection.nytheriacompat$registryAccess.set(this.registryAccess());
            return original.call(enabledFeatures);
        } finally {
            PotionBrewingInjection.nytheriacompat$registryAccess.set(RegistryAccess.EMPTY);
        }
    }

    @WrapOperation(method = "synchronizeTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/resources/ResourceKey;)V"))
    private void nytheriacompat$trySynchronizeWithNeoPacket(PlayerList instance, Packet<?> packet, ResourceKey<Level> dimension, Operation<Void> original, @Local(argsOnly = true) ServerLevel level) {
        if (packet instanceof ClientboundSetTimePacket originalPacket) {
            var neoPacket = new ClientboundCustomSetTimePayload(originalPacket.getGameTime(), originalPacket.getDayTime(), level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT), level.getDayTimeFraction(), level.getDayTimePerTick());
            for (ServerPlayer player : this.getPlayerList().getPlayers()) {
                if (player.level().dimension() == dimension) {
                    if (player.connection.hasChannel(ClientboundCustomSetTimePayload.TYPE)) {
                        player.connection.send(neoPacket);
                    } else {
                        player.connection.send(originalPacket);
                    }
                }
            }
        } else {
            original.call(instance, packet, dimension);
        }
    }

    @Inject(method = "method_29440", at = @At("TAIL"))
    private void nytheriacompat$fixCommandsNotUpdating(Collection collection, MinecraftServer.ReloadableResources reloadableResources, CallbackInfo ci) {
        this.getPlayerList().getPlayers().forEach(this.getPlayerList()::sendPlayerPermissionLevel);
    }

    // Nytheria Compat TODO: do we need pack stuff?

    @Unique private Map<ResourceKey<Level>, long[]> perWorldTickTimes = Maps.newIdentityHashMap();

    @Override
    public long[] getTickTime(ResourceKey<Level> dim) {
        return perWorldTickTimes.get(dim);
    }

    @Override
    public MinecraftServer.ReloadableResources getServerResources() {
        return this.resources;
    }

    // Tick Events implemented via Fabric API
}
