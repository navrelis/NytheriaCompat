/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.network;

import net.minecraft.network.PacketEncoder;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.network.PacketEncoderInjection;

@Mixin(PacketEncoder.class)
public abstract class PacketEncoderInject<T extends PacketListener> implements PacketEncoderInjection<T> {
    @Shadow @Final private ProtocolInfo<T> protocolInfo;

    @Override
    public ProtocolInfo<T> getProtocolInfo() {
        return this.protocolInfo;
    }
}
