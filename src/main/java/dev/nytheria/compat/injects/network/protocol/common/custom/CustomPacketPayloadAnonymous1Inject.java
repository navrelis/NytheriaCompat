package dev.nytheria.compat.injects.network.protocol.common.custom;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.helpers.StupidWorkarounds;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// Nytheria Compat: this used to be nested in a CustomPacketPayloadInject interface mixin that added CustomPacketPayloadInjection
//  (toVanillaClientbound/toVanillaServerbound) to CustomPacketPayload. That interface is already injected by the class
//  tweaker (nytheria_compat.classtweaker, applied by Fabric Loader when the class is defined), so the interface mixin only made
//  CustomPacketPayload a mixin target, and Fabric mods that load it while Mixin prepares its configs (Polymorph's
//  IntegratedMixinPlugin) then crashed the game with MixinTargetAlreadyLoadedException.
@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")
public abstract class CustomPacketPayloadAnonymous1Inject {
    @Unique private final ConnectionProtocol nytheriacompat$protocol = StupidWorkarounds.nytheriacompat$protocol.get();
    @Unique private final PacketFlow nytheriacompat$packetFlow = StupidWorkarounds.nytheriacompat$packetFlow.get();

    @WrapOperation(method = "findCodec", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;create(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/network/codec/StreamCodec;"))
    private <B extends FriendlyByteBuf> StreamCodec<? super B, ? extends CustomPacketPayload> nytheriacompat$tryCreateNeoCodecIfPossible(CustomPacketPayload.FallbackProvider<B> instance, ResourceLocation resourceLocation, Operation<StreamCodec<B, ? extends CustomPacketPayload>> original) {
        if (nytheriacompat$protocol == null || nytheriacompat$packetFlow == null)
            return original.call(instance, resourceLocation);

        var codec = NetworkRegistry.getCodec(resourceLocation, nytheriacompat$protocol, nytheriacompat$packetFlow);
        if (codec == null)
            return original.call(instance, resourceLocation);

        return codec;
    }
}
