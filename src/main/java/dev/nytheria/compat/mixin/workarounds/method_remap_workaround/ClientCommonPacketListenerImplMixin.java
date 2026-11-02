/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.workarounds.method_remap_workaround;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.workarounds.ICommonPacketListenerWorkaround;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;

@Implements(@Interface(iface = ICommonPacketListenerWorkaround.class, prefix = "nytheriacompat$i$", remap = Interface.Remap.NONE))
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {
    @Shadow
    public abstract void send(Packet<?> packet);

    @Intrinsic
    public void nytheriacompat$i$send(Packet<?> packet) {
        this.send(packet);
    }
}
