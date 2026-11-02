// TRACKED HASH: 65eab7af6923cfe40b811ec9f2b77f27d0284455
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.network;

import net.neoforged.neoforge.common.extensions.IFriendlyByteBufExtension;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.network.FriendlyByteBuf;

@Mixin(FriendlyByteBuf.class)
public abstract class FriendlyByteBufInject implements IFriendlyByteBufExtension {

    // Nytheria Compat: Capacity size limiting implemented by Fabric API

}
