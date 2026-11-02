/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on NeoForge by NeoForged, as ported by KiltMC (see VENDORED.md). Licensed under the GNU LGPL 2.1.
 */

package net.neoforged.neoforge.network.payload;

import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Deprecated(forRemoval = true) // Nytheria Compat: Use RegistrationPayload instead.
public record MinecraftRegisterPayload(Set<ResourceLocation> newChannels) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("register");
    public static final Type<MinecraftRegisterPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MinecraftRegisterPayload> STREAM_CODEC = DinnerboneProtocolUtils.CHANNELS_CODEC.map(MinecraftRegisterPayload::new, MinecraftRegisterPayload::newChannels);

    @Override
    public Type<MinecraftRegisterPayload> type() {
        return TYPE;
    }
}
