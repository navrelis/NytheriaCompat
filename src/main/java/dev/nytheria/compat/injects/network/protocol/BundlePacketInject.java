/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.network.protocol;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.neoforged.neoforge.network.bundle.BundlePacketUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BundlePacket.class)
public abstract class BundlePacketInject<T extends PacketListener> {
    @Shadow @Final @Mutable private Iterable<Packet<? super T>> packets;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nytheriacompat$flattenBundlePackets(Iterable<Packet<? super T>> packets, CallbackInfo ci) {
        this.packets = BundlePacketUtils.flatten(this.packets);
    }
}
