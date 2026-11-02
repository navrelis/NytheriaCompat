/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.network;

import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.injections.server.network.CommonListenerCookieInjection;

@Mixin(CommonListenerCookie.class)
public abstract class CommonListenerCookieInject implements CommonListenerCookieInjection {
    private ConnectionType connectionType = ConnectionType.OTHER;

    @Override
    public ConnectionType connectionType() {
        return this.connectionType;
    }

    @Override
    public void nytheriacompat$setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }
}
