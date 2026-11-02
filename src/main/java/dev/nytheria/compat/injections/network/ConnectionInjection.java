/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.network;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.ProtocolInfo;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;


@FabricInjectedInterface(Connection.class)
public interface ConnectionInjection {
    default Channel channel() {
        throw new RuntimeException("mixin, why didn't you add this");
    }

    default ProtocolInfo<?> getInboundProtocol() {
        throw NytheriaCompatHelper.createMixinException(ConnectionInjection.class, "getInboundProtocol");
    }
}
