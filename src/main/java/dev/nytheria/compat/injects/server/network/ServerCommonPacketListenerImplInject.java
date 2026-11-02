/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.network;

import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplInject implements ServerCommonPacketListener {
    @Shadow @Final protected Connection connection;
    @Shadow protected abstract GameProfile playerProfile();
    @Shadow private int latency;
    @Shadow @Final private boolean transferred;
    @Shadow @Final protected MinecraftServer server;

    @Unique protected ConnectionType connectionType = ConnectionType.OTHER;

    @Shadow public void handleCustomPayload(ServerboundCustomPayloadPacket serverboundCustomPayloadPacket) {
        throw new IllegalStateException();
    }

    @Shadow public void handlePong(ServerboundPongPacket serverboundPongPacket) {
        throw new IllegalStateException();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nytheriacompat$setConnectionTypeFromCookie(MinecraftServer server, Connection connection, CommonListenerCookie cookie, CallbackInfo ci) {
        this.connectionType = cookie.connectionType();
    }

    /*@Inject(method = "send", at = @At("HEAD")) // Nytheria Compat: no.
    private void nytheriacompat$validateNetworkPacket(Packet<?> packet, CallbackInfo ci) {
        NetworkRegistry.checkPacket(packet, this);
    }*/

    protected CommonListenerCookie createCookie(ClientInformation info, ConnectionType type) {
        var cookie = new CommonListenerCookie(this.playerProfile(), this.latency, info, this.transferred);
        cookie.nytheriacompat$setConnectionType(type);
        return cookie;
    }


    @Override
    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public ReentrantBlockableEventLoop<?> getMainThreadEventLoop() {
        return this.server;
    }

    @Override
    public ConnectionType getConnectionType() {
        return this.connectionType;
    }
}
