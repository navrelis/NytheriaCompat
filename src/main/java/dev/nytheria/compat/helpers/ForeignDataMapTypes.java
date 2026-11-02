package dev.nytheria.compat.helpers;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import dev.nytheria.compat.NytheriaCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nytheria Compat: data map types that Fabric mods register through NeoForge Data Pack Extensions (a Fabric port of NeoForge's
 * data maps, nested in Dyed Flames). With Nytheria Compat installed that library's own implementation is switched off (see
 * {@code NeoForgeDataPackExtensionsCompat}) and its types are handed to Nytheria Compat's NeoForge data maps instead, which load,
 * sync and look them up like any NeoForge mod's data maps.
 * <p>
 * On plain Fabric that library's loader reads the files of these types from every pack, so Nytheria Compat's data map loader
 * keeps doing that for them ({@link #isForeign}), while it reads every other data map type only from NeoForge mods (see
 * {@code dev.nytheria.compat.util.NeoForgeSources}).
 */
public final class ForeignDataMapTypes {
    private static final List<DataMapType<?, ?>> PENDING = new ArrayList<>();
    private static final Set<ResourceLocation> IDS = ConcurrentHashMap.newKeySet();
    private static boolean collected = false;

    private ForeignDataMapTypes() {
    }

    public static synchronized void register(DataMapType<?, ?> type) {
        if (collected) {
            NytheriaCompat.Companion.getLogger().error("Nytheria Compat: data map type {} was registered after NeoForge collected the data map types, it will not be loaded", type.id());
            return;
        }

        PENDING.add(type);
        IDS.add(type.id());
    }

    /** Whether the data map type {@code id} was registered by a Fabric mod through NeoForge Data Pack Extensions. */
    public static boolean isForeign(ResourceLocation id) {
        return IDS.contains(id);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized void registerAll(RegisterDataMapTypesEvent event) {
        collected = true;

        for (DataMapType type : PENDING) {
            event.register(type);
        }
    }
}
