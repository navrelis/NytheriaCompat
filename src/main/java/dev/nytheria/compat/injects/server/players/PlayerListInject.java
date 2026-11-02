// TRACKED HASH: 2bb3ae15b22cc5132705ea07857cbe14b92c3de6
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.players;

import java.nio.file.Path;
import java.util.function.Function;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.buffer.ByteBuf;
import net.neoforged.neoforge.attachment.AttachmentSync;
import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.network.RegistryFriendlyByteBufInjection;

import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;

@Mixin(PlayerList.class)
public abstract class PlayerListInject {
    @Shadow @Final private MinecraftServer server;

    @ModifyExpressionValue(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/RegistryFriendlyByteBuf;decorator(Lnet/minecraft/core/RegistryAccess;)Ljava/util/function/Function;"))
    private Function<ByteBuf, RegistryFriendlyByteBuf> nytheriacompat$addConnectionTypeToBuf(Function<ByteBuf, RegistryFriendlyByteBuf> original, @Local ServerGamePacketListenerImpl packetListener) {
        return RegistryFriendlyByteBufInjection.nytheriacompat$wrappedDecorator(packetListener.getConnectionType(), original);
    }

    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundUpdateRecipesPacket;<init>(Ljava/util/Collection;)V"))
    private void nytheriacompat$syncDatapackRegistries(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        if (EventGate.has(OnDatapackSyncEvent.class)) NeoForge.EVENT_BUS.post(new OnDatapackSyncEvent((PlayerList) (Object) this, player));
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void nytheriacompat$firePlayerLoginEvent(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        AttachmentSync.syncInitialPlayerAttachments(player);
        EventHooks.firePlayerLoggedIn(player);
    }

    @Inject(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setHealth(F)V", shift = At.Shift.AFTER))
    private void nytheriacompat$firePlayerRespawnEvent(ServerPlayer player, boolean keepInventory, Entity.RemovalReason reason, CallbackInfoReturnable<ServerPlayer> cir, @Local(ordinal = 1) ServerPlayer newPlayer) {
        // Nytheria Compat: like NeoForge, with the new player (the old one is already removed from the level)
        AttachmentSync.syncInitialPlayerAttachments(newPlayer);
        EventHooks.firePlayerRespawnEvent(newPlayer, keepInventory);
    }

    @Inject(method = "getPlayerAdvancements", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$avoidSetPlayerIfFake(ServerPlayer player, CallbackInfoReturnable<PlayerAdvancements> cir) {
        if (player.isFakePlayer()) {
            Path path = this.server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve("_fake.json");
            cir.setReturnValue(new FakePlayer.FakePlayerAdvancements(this.server.getFixerUpper(), (PlayerList) (Object) this, this.server.getAdvancements(), path, player));
        }
    }

    @Inject(method = "reloadResources", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
    private void nytheriacompat$syncDatapackOnReload(CallbackInfo ci) {
        if (EventGate.has(OnDatapackSyncEvent.class)) NeoForge.EVENT_BUS.post(new OnDatapackSyncEvent((PlayerList) (Object) this, null));
    }
}
