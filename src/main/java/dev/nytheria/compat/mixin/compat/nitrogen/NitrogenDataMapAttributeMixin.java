package dev.nytheria.compat.mixin.compat.nitrogen;

import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import dev.nytheria.compat.helpers.NitrogenCompat;

/**
 * Nytheria Compat: The data map port in The Aether / Nitrogen Internals for Fabric stores the data maps a client knows in the
 * Netty channel attribute {@code AttributeKey.valueOf("neoforge:known_data_maps")}, which is the very same key
 * NeoForge's {@code RegistryManager.ATTRIBUTE_KNOWN_DATA_MAPS} uses ({@code AttributeKey.valueOf} returns the
 * existing key for a name). Both negotiations would then overwrite each other's list on the same connection, and one
 * side would stop syncing data maps the client does know. The copy gets its own key here, see
 * {@link NitrogenNeoForgeIdsMixin} for the rest.
 */
@Pseudo
@IfModLoaded("nitrogen_internals")
@Mixin(targets = {
    // The Aether 1.5.x for Fabric
    "com.aetherteam.aetherfabric.registries.RegistryManager",
    // Nitrogen Internals 1.1.22+ for Fabric
    "com.aetherteam.nitrogen.fabric.registries.RegistryManager"
}, remap = false)
public abstract class NitrogenDataMapAttributeMixin {
    @ModifyConstant(method = "<clinit>", constant = @Constant(stringValue = "neoforge:known_data_maps"), require = 0, expect = 0, remap = false)
    private static String nytheriacompat$useOwnAttributeKey(String name) {
        return NitrogenCompat.remapId(name);
    }
}
