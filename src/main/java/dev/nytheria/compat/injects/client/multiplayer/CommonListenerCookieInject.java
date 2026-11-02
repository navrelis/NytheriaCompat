/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.multiplayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.client.multiplayer.CommonListenerCookieInjection;

import java.util.Map;

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

    public CommonListenerCookieInject(GameProfile localGameProfile, WorldSessionTelemetryManager telemetryManager, RegistryAccess.Frozen receivedRegistries, FeatureFlagSet enabledFeatures, @Nullable String serverBrand, @Nullable ServerData serverData, @Nullable Screen postDisconnectScreen, Map<ResourceLocation, byte[]> serverCookies, @Nullable ChatComponent.State chatState, boolean strictErrorHandling, Map<String, String> customReportDetails, ServerLinks serverLinks) {}

    @CreateInitializer
    public CommonListenerCookieInject(GameProfile localGameProfile, WorldSessionTelemetryManager telemetryManager, RegistryAccess.Frozen receivedRegistries, FeatureFlagSet enabledFeatures, @Nullable String serverBrand, @Nullable ServerData serverData, @Nullable Screen postDisconnectScreen, Map<ResourceLocation, byte[]> serverCookies, @Nullable ChatComponent.State chatState, boolean strictErrorHandling, Map<String, String> customReportDetails, ServerLinks serverLinks, ConnectionType connectionType) {
        this(localGameProfile, telemetryManager, receivedRegistries, enabledFeatures, serverBrand, serverData, postDisconnectScreen, serverCookies, chatState, strictErrorHandling, customReportDetails, serverLinks);
        this.connectionType = connectionType;
    }
}
