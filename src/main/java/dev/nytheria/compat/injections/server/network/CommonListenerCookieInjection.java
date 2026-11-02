/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.server.network;

import net.neoforged.neoforge.network.connection.ConnectionType;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface CommonListenerCookieInjection {
    default ConnectionType connectionType() {
        throw NytheriaCompatHelper.createMixinException(CommonListenerCookieInjection.class, "connectionType");
    }

    default void nytheriacompat$setConnectionType(ConnectionType connectionType) {
        throw NytheriaCompatHelper.createMixinException(CommonListenerCookieInjection.class, "nytheriacompat$setConnectionType");
    }
}
