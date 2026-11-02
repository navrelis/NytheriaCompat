/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface PacketEncoderInjection<T extends PacketListener> {
    default ProtocolInfo<T> getProtocolInfo() {
        throw NytheriaCompatHelper.createMixinException(PacketEncoderInjection.class, "getProtocolInfo");
    }
}
