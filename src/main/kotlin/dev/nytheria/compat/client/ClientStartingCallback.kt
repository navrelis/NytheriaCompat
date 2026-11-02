/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.client

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.client.Minecraft

/**
 * Event for after Minecraft does its <init>
 */
fun interface ClientStartingCallback {
    fun onClientStarting(mc: Minecraft)

    companion object {
        @JvmField
        val EVENT: Event<ClientStartingCallback> = EventFactory.createArrayBacked(
            ClientStartingCallback::class.java
        ) { callbacks ->
            ClientStartingCallback { client: Minecraft ->
                for (callback in callbacks) {
                    callback.onClientStarting(client)
                }
            }
        };
    }
}