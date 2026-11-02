// TRACKED HASH: b6113e1d44b934ff02eb6b5b554fa8ccbd085e79
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.network;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.*;
import net.neoforged.neoforge.network.connection.ConnectionUtils;
import net.neoforged.neoforge.network.filters.NetworkFilters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.network.ConnectionInjection;

@Mixin(Connection.class)
public abstract class ConnectionInject implements ConnectionInjection {
    @Shadow private Channel channel;

    @Unique @Nullable private ProtocolInfo<?> inboundProtocol;

    @Inject(method = "channelActive", at = @At("TAIL"))
    private void nytheriacompat$setNeoConnection(ChannelHandlerContext channelHandlerContext, CallbackInfo ci) {
        ConnectionUtils.setConnection(channelHandlerContext, (Connection) (Object) this);
    }

    @Inject(method = "channelInactive", at = @At("TAIL"))
    private void nytheriacompat$removeNeoConnection(ChannelHandlerContext channelHandlerContext, CallbackInfo ci) {
        ConnectionUtils.removeConnection(channelHandlerContext);
    }

    @Definition(id = "packetListener", field = "Lnet/minecraft/network/Connection;packetListener:Lnet/minecraft/network/PacketListener;")
    @Definition(id = "packetInfo", local = @Local(type = PacketListener.class, argsOnly = true))
    @Expression("this.packetListener = packetInfo")
    @Inject(method = "setupInboundProtocol", at = @At("MIXINEXTRAS:EXPRESSION"))
    private <T extends PacketListener> void nytheriacompat$storeCurrentInboundProtocol(ProtocolInfo<T> protocolInfo, T packetInfo, CallbackInfo ci) {
        this.inboundProtocol = protocolInfo;
    }

    @Inject(method = "method_56328", at = @At("TAIL"))
    private static void nytheriacompat$injectNetworkFiltersIfNecessary(PacketBundleUnpacker packetBundleUnpacker, ChannelHandlerContext channelHandlerContext, CallbackInfo ci) {
        NetworkFilters.injectIfNecessary(ConnectionUtils.getConnection(channelHandlerContext));
    }

    // Nytheria Compat: still not handling dual-stack

    @NotNull
    @Override
    public Channel channel() {
        return this.channel;
    }

    @Override
    public @Nullable ProtocolInfo<?> getInboundProtocol() {
        return this.inboundProtocol;
    }
}