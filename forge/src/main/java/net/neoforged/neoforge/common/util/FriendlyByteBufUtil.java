/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on NeoForge by NeoForged, as ported by KiltMC (see VENDORED.md). Licensed under the GNU LGPL 2.1.
 */

package net.neoforged.neoforge.common.util;

import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

/**
 * Utility class for working with {@link FriendlyByteBuf}s.
 */
public class FriendlyByteBufUtil {
    private FriendlyByteBufUtil() {
        throw new IllegalStateException("Tried to create utility class!");
    }

    /**
     * Writes custom data to a {@link RegistryFriendlyByteBuf}, then returns the written data as a byte array.
     *
     * @param dataWriter     The data writer.
     * @param registryAccess The registry access used by registry dependent writers on the buffer
     * @return The written data.
     */
    public static byte[] writeCustomData(Consumer<RegistryFriendlyByteBuf> dataWriter, RegistryAccess registryAccess) {
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        buf.nytheriacompat$setConnectionType(ConnectionType.NEOFORGE);
        try {
            dataWriter.accept(buf);
            buf.readerIndex(0);
            final byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }
}
