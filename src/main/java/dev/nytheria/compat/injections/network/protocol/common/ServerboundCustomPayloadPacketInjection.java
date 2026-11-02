/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.network.protocol.common;

import com.google.common.collect.Lists;
import net.minecraft.Util;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import dev.nytheria.compat.injections.network.protocol.common.custom.CustomPacketPayloadInjection;

public interface ServerboundCustomPayloadPacketInjection {
    StreamCodec<FriendlyByteBuf, ServerboundCustomPayloadPacket> CONFIG_STREAM_CODEC = CustomPacketPayloadInjection.codec(
        id -> DiscardedPayload.codec(id, 32767),
        Util.make(Lists.newArrayList(new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC)), $ -> {}),
        ConnectionProtocol.CONFIGURATION, PacketFlow.SERVERBOUND
    )
        .map(ServerboundCustomPayloadPacket::new, ServerboundCustomPayloadPacket::payload);

    static StreamCodec<FriendlyByteBuf, ServerboundCustomPayloadPacket> nytheriacompat$getConfigStreamCodec(StreamCodec<FriendlyByteBuf, ServerboundCustomPayloadPacket> original) {
        return new StreamCodec<>() {
            @Override
            public ServerboundCustomPayloadPacket decode(FriendlyByteBuf buf) {
                var currentIndex = buf.readerIndex();
                // We want to try to get at least some mod compatibility...
                var result = NetworkRegistry.getCodec(buf.readResourceLocation(), ConnectionProtocol.CONFIGURATION, PacketFlow.SERVERBOUND);

                if (result != null) {
                    return new ServerboundCustomPayloadPacket(result.decode(buf));
                }

                // We failed to get this packet, fall back to the old one!
                buf.readerIndex(currentIndex);

                return original.decode(buf);
            }

            @Override
            public void encode(FriendlyByteBuf buf, ServerboundCustomPayloadPacket packet) {
                var currentIndex = buf.writerIndex();
                StreamCodec result = NetworkRegistry.getCodec(packet.type().id(), ConnectionProtocol.CONFIGURATION, PacketFlow.SERVERBOUND);

                // We want to try to get at least some mod compatibility...
                if (result != null) {
                    buf.writeResourceLocation(packet.type().id());
                    result.encode(buf, packet.payload());
                    return;
                }

                // We failed to write this packet, fall back to the old one!
                buf.writerIndex(currentIndex);

                original.encode(buf, packet);
            }
        };
    }
}
