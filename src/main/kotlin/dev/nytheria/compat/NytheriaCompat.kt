/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat

import com.google.gson.GsonBuilder
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import dev.nytheria.compat.bridge.PlayerBridge
import dev.nytheria.compat.bridge.ServerBridge
import dev.nytheria.compat.client.NytheriaCompatClient
import dev.nytheria.compat.loader.NytheriaCompatLoader

class NytheriaCompat : ModInitializer {
    override fun onInitialize() {
        PlayerBridge.register()
        ServerBridge.register()
    }

    companion object {
        /**
         * The Fabric mod ID of this jar (fabric.mod.json "id"). Also used as the Knit loader ID, as the namespace
         * for registry/tag/layer IDs and as the MixinExtras Share namespace.
         */
        const val MOD_ID = "nytheria_compat"

        lateinit var instance: NytheriaCompat
        val logger: Logger = LoggerFactory.getLogger("Nytheria Compat")
        val loader: NytheriaCompatLoader
            get() = NytheriaCompatLoader.instance
        val gson = GsonBuilder().setPrettyPrinting().create()

        fun load(onServer: Boolean) {
            // config load should be here
            if (!onServer) {
                NytheriaCompatClient.lateRegisterEvents()
            }
        }
    }
}
