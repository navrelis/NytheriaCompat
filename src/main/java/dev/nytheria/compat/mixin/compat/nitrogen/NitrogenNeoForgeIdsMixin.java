package dev.nytheria.compat.mixin.compat.nitrogen;

import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import dev.nytheria.compat.helpers.NitrogenCompat;

/**
 * Nytheria Compat: The Fabric port of The Aether (and its library Nitrogen Internals) ships its own copy of NeoForge's
 * data map networking and complex entity spawn packet, and registers them under the <b>{@code neoforge}</b>
 * namespace: {@code neoforge:known_registry_data_maps}, {@code neoforge:known_registry_data_maps_reply},
 * {@code neoforge:registry_data_map_sync}, {@code neoforge:advanced_add_entity}, plus the configuration task
 * {@code neoforge:registry_data_map_negotiation}. Nytheria Compat bundles the real NeoForge, which registers the very same
 * ids with Fabric API, so whichever registers second crashes with "Packet type ... is already registered!"
 * (see https://github.com/The-Aether-Team/Nitrogen/issues/38).
 * <p>
 * The copies are private to the Aether (their payload classes are their own, not NeoForge's), so they are moved to
 * the {@code nitrogen_internals} namespace here. NeoForge keeps its own ids, and both systems work side by side.
 * The ids only change when Nytheria Compat is installed, which has to be on both the client and the server anyway.
 * <p>
 * Only the {@code "neoforge"} string constant in the static initializers of these classes is touched. The data map
 * <i>type</i> ids ({@code neoforge:compostables}, {@code neoforge:furnace_fuels}) are left alone on purpose, they live
 * in the Aether's own data map registry and decide which {@code data_maps} files it reads. The targets cover the package
 * the port lives in within Aether 1.5.x ({@code com.aetherteam.aetherfabric}) and the one it moved to in later
 * Nitrogen Internals builds ({@code com.aetherteam.nitrogen.fabric}). Classes that don't exist are simply never
 * transformed, and the injector is optional, so an Aether update can't crash the game because of this mixin.
 */
@Pseudo
@IfModLoaded("nitrogen_internals")
@Mixin(targets = {
    // The Aether 1.5.x for Fabric (bundles Nitrogen Internals 1.1.21, the port lives in the Aether jar)
    "com.aetherteam.aetherfabric.network.payload.AdvancedAddEntityPayload",
    "com.aetherteam.aetherfabric.network.payload.KnownRegistryDataMapsPayload",
    "com.aetherteam.aetherfabric.network.payload.KnownRegistryDataMapsReplyPayload",
    "com.aetherteam.aetherfabric.network.payload.RegistryDataMapSyncPayload",
    "com.aetherteam.aetherfabric.network.tasks.RegistryDataMapNegotiation",
    // Nitrogen Internals 1.1.22+ for Fabric (the port moved into Nitrogen)
    "com.aetherteam.nitrogen.fabric.network.payload.AdvancedAddEntityPayload",
    "com.aetherteam.nitrogen.fabric.network.payload.KnownRegistryDataMapsPayload",
    "com.aetherteam.nitrogen.fabric.network.payload.KnownRegistryDataMapsReplyPayload",
    "com.aetherteam.nitrogen.fabric.network.payload.RegistryDataMapSyncPayload",
    "com.aetherteam.nitrogen.fabric.network.tasks.RegistryDataMapNegotiation"
}, remap = false)
public abstract class NitrogenNeoForgeIdsMixin {
    @ModifyConstant(method = "<clinit>", constant = @Constant(stringValue = "neoforge"), require = 0, expect = 0, remap = false)
    private static String nytheriacompat$moveOutOfNeoForgeNamespace(String namespace) {
        return NitrogenCompat.remapNamespace(namespace);
    }
}
