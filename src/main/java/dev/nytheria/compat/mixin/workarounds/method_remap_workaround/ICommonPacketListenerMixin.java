/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.workarounds.method_remap_workaround;

import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.workarounds.ICommonPacketListenerWorkaround;

import net.minecraft.network.protocol.Packet;

@Mixin(ICommonPacketListener.class)
public interface ICommonPacketListenerMixin extends ICommonPacketListenerWorkaround {
    @Override
    void send(Packet<?> packet);
}
