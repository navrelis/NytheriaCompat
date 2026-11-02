/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.network.protocol.common.custom;

import java.util.List;

import dev.nytheria.compat.helpers.StupidWorkarounds;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface CustomPacketPayloadInjection {
    static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload> codec(
        final CustomPacketPayload.FallbackProvider<B> fallbackProvider, List<CustomPacketPayload.TypeAndCodec<? super B, ?>> payloads,
        ConnectionProtocol protocol, PacketFlow packetFlow
    ) {
        StupidWorkarounds.nytheriacompat$protocol.set(protocol);
        StupidWorkarounds.nytheriacompat$packetFlow.set(packetFlow);

        try {
            return CustomPacketPayload.codec(fallbackProvider, payloads);
        } finally {
            StupidWorkarounds.nytheriacompat$protocol.remove();
            StupidWorkarounds.nytheriacompat$packetFlow.remove();
        }
    }

    default ClientboundCustomPayloadPacket toVanillaClientbound() {
        return new ClientboundCustomPayloadPacket((CustomPacketPayload) this);
    }

    default ServerboundCustomPayloadPacket toVanillaServerbound() {
        return new ServerboundCustomPayloadPacket((CustomPacketPayload) this);
    }
}
