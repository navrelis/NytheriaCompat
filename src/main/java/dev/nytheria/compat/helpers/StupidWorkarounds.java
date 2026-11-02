/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;

public interface StupidWorkarounds {
    // for CustomPacketPayloadInjection - because CreativeCore just doesn't work properly otherwise, due to reflection bullshit. it's dumb.
    ThreadLocal<ConnectionProtocol> nytheriacompat$protocol = new ThreadLocal<>();
    ThreadLocal<PacketFlow> nytheriacompat$packetFlow = new ThreadLocal<>();

    Map<FluidType, IClientFluidTypeExtensions> nytheriacompat$fabricFluidExtensions = Collections.synchronizedMap(new HashMap<>());
}
