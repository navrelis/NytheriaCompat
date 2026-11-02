/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.network.protocol.configuration;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.nytheria.compat.injections.network.protocol.common.ServerboundCustomPayloadPacketInjection;

@Mixin(ConfigurationProtocols.class)
public abstract class ConfigurationProtocolsInject {
    @ModifyExpressionValue(method = "method_56513", at = @At(value = "FIELD", target = "Lnet/minecraft/network/protocol/common/ServerboundCustomPayloadPacket;STREAM_CODEC:Lnet/minecraft/network/codec/StreamCodec;"), require = 0)
    private static StreamCodec<FriendlyByteBuf, ServerboundCustomPayloadPacket> nytheriacompat$useProtocolAwareStreamCodec(StreamCodec<FriendlyByteBuf, ServerboundCustomPayloadPacket> original) {
        return ServerboundCustomPayloadPacketInjection.nytheriacompat$getConfigStreamCodec(original);
    }
}
