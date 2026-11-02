/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.client

import com.google.common.collect.ImmutableMap
import io.github.fabricators_of_create.porting_lib.event.client.TextureAtlasStitchedEvent
import io.github.fabricators_of_create.porting_lib.models.geometry.RegisterGeometryLoadersCallback
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.neoforged.fml.ModLoader
import net.neoforged.neoforge.client.ClientHooks
import net.neoforged.neoforge.client.event.ModelEvent
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader
import net.neoforged.neoforge.event.EventHooks
import dev.nytheria.compat.mixin.GeometryLoaderManagerAccessor

@Suppress("removal")
class NytheriaCompatClient : ClientModInitializer {
    override fun onInitializeClient() {
        registerFabricEvents()

        hasInitialized = true
    }

    private fun registerFabricEvents() {
        val mc = Minecraft.getInstance()

        /*ParticleManagerRegistrationCallback.EVENT.register {
            NytheriaCompat.loader.postEvent(RegisterParticleProvidersEvent(Minecraft.getInstance().particleEngine))
        }*/

        // Nytheria Compat: ItemTooltipEvent is fired by ItemStackInject#onItemTooltip. Fabric's ItemTooltipCallback has no player,
        //  so the bridge here fired it with a null player (Curios reads the player for its slot/attribute tooltips).

        TextureAtlasStitchedEvent.EVENT.register { event ->
            val forgeEvent = net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent(event.atlas)
            ModLoader.postEventWrapContainerInModOrder(forgeEvent)
        }

        RegisterGeometryLoadersCallback.EVENT.register { map ->
            shouldPostGeoLoaders = true

            val neoMap = mutableMapOf<ResourceLocation, IGeometryLoader<*>>()
            ModLoader.postEventWrapContainerInModOrder(ModelEvent.RegisterGeometryLoaders(neoMap))

            // Convert, because it just works.
            map.putAll(neoMap)
        }

        ScreenEvents.BEFORE_INIT.register { client, screen, width, height ->
            // Nytheria Compat: the ScreenEvent.MouseButtonPressed / MouseButtonReleased / MouseScrolled events are fired by
            //  MouseHandlerInject around the same Screen calls that Fabric's ScreenMouseEvents wrap. Bridging those
            //  as well fired every screen mouse event twice.

            ScreenKeyboardEvents.allowKeyPress(screen).register { _, key, scanCode, modifiers ->
                !ClientHooks.onScreenKeyPressedPre(screen, key, scanCode, modifiers)
            }

            ScreenKeyboardEvents.afterKeyPress(screen).register { _, key, scanCode, modifiers ->
                ClientHooks.onScreenKeyPressedPost(screen, key, scanCode, modifiers)
            }

            ScreenKeyboardEvents.allowKeyRelease(screen).register { _, key, scanCode, modifiers ->
                !ClientHooks.onScreenKeyReleasedPre(screen, key, scanCode, modifiers)
            }

            ScreenKeyboardEvents.afterKeyRelease(screen).register { _, key, scanCode, modifiers ->
                ClientHooks.onScreenKeyReleasedPost(screen, key, scanCode, modifiers)
            }
        }

        /*RenderHandCallback.EVENT.register { event ->
            val forgeEvent = RenderHandEvent(event.hand, event.poseStack, event.multiBufferSource, event.packedLight, event.partialTicks, event.pitch, event.swingProgress, event.equipProgress, event.itemStack)
            NeoForge.EVENT_BUS.post(forgeEvent)

            if (forgeEvent.isCanceled)
                event.isCanceled = true
        }*/

        ClientTickEvents.START_CLIENT_TICK.register {
            ClientHooks.fireClientTickPre()
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            ClientHooks.fireClientTickPost()
        }

        ClientTickEvents.START_WORLD_TICK.register {
            EventHooks.fireLevelTickPre(it) { true }
        }

        ClientTickEvents.END_WORLD_TICK.register {
            EventHooks.fireLevelTickPost(it) { true }
        }

        /*ClientWorldEvents.LOAD.register { client, level ->
            NeoForge.EVENT_BUS.post(LevelEvent.Load(level))
        }*/

        /*ClientWorldEvents.UNLOAD.register { client, level ->
            NeoForge.EVENT_BUS.post(LevelEvent.Unload(level))
        }*/
    }

    companion object {
        var hasInitialized = false
            private set

        private var shouldPostGeoLoaders = false

        fun lateRegisterEvents() {
            if (shouldPostGeoLoaders) {
                val map = GeometryLoaderManagerAccessor.getLoaders().toMutableMap()
                val neoMap = mutableMapOf<ResourceLocation, IGeometryLoader<*>>()
                ModLoader.postEventWrapContainerInModOrder(ModelEvent.RegisterGeometryLoaders(neoMap))

                map.putAll(neoMap)
                GeometryLoaderManagerAccessor.setLoaders(ImmutableMap.copyOf(map))
                GeometryLoaderManagerAccessor.setLoaderList(map.keys.joinToString(", ") { it.toString() })
            }
        }
    }
}
