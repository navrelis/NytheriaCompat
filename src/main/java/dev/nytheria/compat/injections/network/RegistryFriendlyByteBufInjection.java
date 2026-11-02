/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface RegistryFriendlyByteBufInjection {
    static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess registryAccess, ConnectionType connectionType) {
        return buf -> {
            var friendlyBuf = new RegistryFriendlyByteBuf(buf, registryAccess);
            friendlyBuf.nytheriacompat$setConnectionType(connectionType);

            return friendlyBuf;
        };
    }

    static Function<ByteBuf, RegistryFriendlyByteBuf> nytheriacompat$wrappedDecorator(ConnectionType connectionType, Function<ByteBuf, RegistryFriendlyByteBuf> wrapped) {
        return buf -> {
            var original = wrapped.apply(buf);
            original.nytheriacompat$setConnectionType(connectionType);
            return original;
        };
    }

    default ConnectionType getConnectionType() {
        throw NytheriaCompatHelper.createMixinException(RegistryFriendlyByteBufInjection.class, "getConnectionType");
    }

    default void nytheriacompat$setConnectionType(ConnectionType connectionType) {
        throw NytheriaCompatHelper.createMixinException(RegistryFriendlyByteBufInjection.class, "nytheriacompat$setConnectionType");
    }
}
