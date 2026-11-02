/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.level;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.server.level.ServerEntityInjection;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

@Mixin(ServerEntity.class)
public abstract class ServerEntityInject implements ServerEntityInjection {
    @Shadow @Final private Entity entity;

    @Shadow
    public abstract void sendPairingData(ServerPlayer player, Consumer<Packet<ClientGamePacketListener>> consumer);

    @Unique private PacketAndPayloadAcceptor<ClientGamePacketListener> nytheriacompat$payloadAcceptor;
    @Unique private final AtomicBoolean nytheriacompat$isDeferringPairingData = new AtomicBoolean(false);

    @WrapOperation(method = "addPairing", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerEntity;sendPairingData(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V"))
    private void nytheriacompat$wrapWithPacketAndPayloadAcceptor(ServerEntity instance, ServerPlayer player, Consumer<Packet<ClientGamePacketListener>> consumer, Operation<Void> original) {
        this.nytheriacompat$payloadAcceptor = new PacketAndPayloadAcceptor<>((Consumer<Packet<? super ClientGamePacketListener>>) (Object) consumer); // why?
        original.call(instance, player, consumer);
        this.nytheriacompat$payloadAcceptor = null;
    }

    @Inject(method = "addPairing", at = @At("TAIL"))
    private void nytheriacompat$callStartEntityTracking(ServerPlayer player, CallbackInfo ci) {
        EventHooks.onStartEntityTracking(this.entity, player);
    }

    @Unique
    public void neoforge$sendPairingData(ServerPlayer player, PacketAndPayloadAcceptor<ClientGamePacketListener> acceptor) {
        if (this.nytheriacompat$isDeferringPairingData.getAndSet(false)) {
            return;
        }

        this.sendPairingData(player, acceptor);
    }

    @Override
    public void sendPairingData(ServerPlayer player, PacketAndPayloadAcceptor<ClientGamePacketListener> acceptor) {
        this.nytheriacompat$payloadAcceptor = acceptor;
        this.sendPairingData(player, acceptor::accept);
        this.nytheriacompat$payloadAcceptor = null;
    }

    @Inject(method = "sendPairingData", at = @At("HEAD"))
    private void nytheriacompat$tryHandleFunnyMixins(ServerPlayer player, Consumer<Packet<ClientGamePacketListener>> consumer, CallbackInfo ci) {
        try {
            this.nytheriacompat$isDeferringPairingData.set(true);

            this.neoforge$sendPairingData(player, Objects.requireNonNullElseGet(this.nytheriacompat$payloadAcceptor,
                () -> new PacketAndPayloadAcceptor<>((Consumer<Packet<? super ClientGamePacketListener>>) (Object) consumer))
            );
        } finally {
            this.nytheriacompat$isDeferringPairingData.set(false);
        }
    }

    @Definition(id = "trackedDataValues", field = "Lnet/minecraft/server/level/ServerEntity;trackedDataValues:Ljava/util/List;")
    @Expression("this.trackedDataValues != null")
    @Inject(method = "sendPairingData", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void nytheriacompat$sendPairingDataWithAcceptor(ServerPlayer player, Consumer<Packet<ClientGamePacketListener>> consumer, CallbackInfo ci) {
        if (this.nytheriacompat$payloadAcceptor != null) {
            this.entity.sendPairingData(player, this.nytheriacompat$payloadAcceptor::accept);
        } else {
            this.entity.sendPairingData(player, payload -> ((Consumer) consumer).accept(new ClientboundCustomPayloadPacket(payload)));
        }
    }
}
