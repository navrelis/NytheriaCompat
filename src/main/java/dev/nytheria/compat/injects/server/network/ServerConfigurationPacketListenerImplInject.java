/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.network.RegistryFriendlyByteBufInjection;
import dev.nytheria.compat.workarounds.IServerConfigurationPacketListenerWorkaround;

import java.util.Map;
import java.util.Queue;
import java.util.function.Function;

@Implements({
    @Interface(iface = ServerConfigurationPacketListener.class, prefix = "nytheriacompat$i$"),
    @Interface(iface = IServerConfigurationPacketListenerWorkaround.class, prefix = "nytheriacompat$j$")
})
@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplInject extends ServerCommonPacketListenerImplInject implements ServerConfigurationPacketListener {
    @Shadow @Final private Queue<ConfigurationTask> configurationTasks;
    @Shadow public abstract void finishCurrentTask(ConfigurationTask.Type taskType);

    @Inject(method = "addOptionalTasks", at = @At("TAIL"))
    private void nytheriacompat$gatherConfigurationTasks(CallbackInfo ci) {
        this.configurationTasks.addAll(ModLoader.postEventWithReturn(new RegisterConfigurationTasksEvent(this)).getConfigurationTasks());
    }

    @Intrinsic(displace = true)
    public void nytheriacompat$i$handleCustomPayload(ServerboundCustomPayloadPacket packet) {
        if (packet.payload() instanceof ModdedNetworkQueryPayload payload) {
            this.connectionType = ConnectionType.NEOFORGE;
            NetworkRegistry.initializeNeoForgeConnection(this, payload.queries());
            return;
        }

        super.handleCustomPayload(packet);
    }

    @Intrinsic(displace = true)
    public void nytheriacompat$i$handlePong(ServerboundPongPacket packet) {
        super.handlePong(packet);

        if (packet.getId() == 0) {
            if (!this.connectionType.isNeoForge() && !NetworkRegistry.initializeOtherConnection(this)) {
                return;
            }

            // this.runConfiguration();
            // Nytheria Compat TODO: do we handle pong behaviour?
        }
    }

    @ModifyExpressionValue(method = "handleConfigurationFinished", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/RegistryFriendlyByteBuf;decorator(Lnet/minecraft/core/RegistryAccess;)Ljava/util/function/Function;"))
    private Function<ByteBuf, RegistryFriendlyByteBuf> nytheriacompat$appendConnectionTypeToDecorator(Function<ByteBuf, RegistryFriendlyByteBuf> original) {
        return RegistryFriendlyByteBufInjection.nytheriacompat$wrappedDecorator(this.connectionType, original);
    }

    @Inject(method = "handleConfigurationFinished", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setupOutboundProtocol(Lnet/minecraft/network/ProtocolInfo;)V", shift = At.Shift.AFTER))
    private void nytheriacompat$handleConfigurationFinish(ServerboundFinishConfigurationPacket packet, CallbackInfo ci) {
        if (this.connectionType == ConnectionType.OTHER) {
            NetworkRegistry.initializeNeoForgeConnection(this, Map.of());
        }

        NetworkRegistry.onConfigurationFinished(this);
    }

    @ModifyExpressionValue(method = "handleConfigurationFinished", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerConfigurationPacketListenerImpl;createCookie(Lnet/minecraft/server/level/ClientInformation;)Lnet/minecraft/server/network/CommonListenerCookie;"))
    private CommonListenerCookie nytheriacompat$attachConnectionTypeToCookie(CommonListenerCookie original) {
        original.nytheriacompat$setConnectionType(this.connectionType);
        return original;
    }

    @Intrinsic
    public void nytheriacompat$j$finishCurrentTask(ConfigurationTask.Type task) {
        this.finishCurrentTask(task);
    }
}
