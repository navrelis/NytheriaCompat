/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.multiplayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.neoforged.neoforge.common.extensions.IClientCommonPacketListenerExtension;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplInject implements ClientCommonPacketListener, IClientCommonPacketListenerExtension {
    @Shadow
    @Final
    protected Connection connection;
    protected ConnectionType connectionType = ConnectionType.OTHER;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nytheriacompat$setConnectionTypeBasedOnCookie(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
        this.connectionType = commonListenerCookie.connectionType();
    }

    /*@Inject(method = "send", at = @At("HEAD")) // Nytheria Compat: no.
    private void nytheriacompat$validateNetworkPacket(Packet<?> packet, CallbackInfo ci) {
        NetworkRegistry.checkPacket(packet, this);
    }*/

    @Override
    public Connection getConnection() {
        return this.connection;
    }
}
