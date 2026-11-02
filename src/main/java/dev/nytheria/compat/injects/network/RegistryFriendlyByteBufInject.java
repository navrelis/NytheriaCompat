/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.network.RegistryFriendlyByteBufInjection;

import java.util.function.Function;

@Mixin(RegistryFriendlyByteBuf.class)
public abstract class RegistryFriendlyByteBufInject implements RegistryFriendlyByteBufInjection {
    private ConnectionType connectionType = ConnectionType.OTHER;

    public RegistryFriendlyByteBufInject(ByteBuf byteBuf, RegistryAccess registryAccess) {}

    @CreateInitializer
    public RegistryFriendlyByteBufInject(ByteBuf byteBuf, RegistryAccess registryAccess, ConnectionType connectionType) {
        this(byteBuf, registryAccess);
        this.connectionType = connectionType;
    }

    @Override
    public ConnectionType getConnectionType() {
        return this.connectionType;
    }

    @Override
    public void nytheriacompat$setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    @CreateStatic
    private static Function<ByteBuf, RegistryFriendlyByteBuf> decorator(RegistryAccess registryAccess, ConnectionType connectionType) {
        return RegistryFriendlyByteBufInjection.decorator(registryAccess, connectionType);
    }
}
