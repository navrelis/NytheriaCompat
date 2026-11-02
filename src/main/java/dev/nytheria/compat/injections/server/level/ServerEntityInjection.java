/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.server.level;

import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;

public interface ServerEntityInjection {
    default void sendPairingData(ServerPlayer player, PacketAndPayloadAcceptor<ClientGamePacketListener> acceptor) {
        throw NytheriaCompatHelper.createMixinException(ServerEntityInjection.class, "sendPairingData");
    }
}
