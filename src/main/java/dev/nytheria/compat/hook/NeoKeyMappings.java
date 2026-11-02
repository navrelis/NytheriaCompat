package dev.nytheria.compat.hook;

import net.minecraft.client.KeyMapping;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The key mappings of NeoForge mods. A {@link KeyMapping} is a vanilla class, so {@link Scope} cannot tell whose it is;
 * a NeoForge mod hands its mappings to {@code RegisterKeyMappingsEvent}, which only NeoForge mods listen to. Key handling
 * is NeoForge's (several bindings per key, conflict contexts) only for a key with such a mapping; any other key is vanilla's.
 */
public final class NeoKeyMappings {
    // KeyMapping has no equals or hashCode of its own, so this is an identity set.
    private static final Set<KeyMapping> MAPPINGS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private NeoKeyMappings() {
    }

    public static void add(KeyMapping mapping) {
        MAPPINGS.add(mapping);
    }

    public static boolean contains(KeyMapping mapping) {
        return MAPPINGS.contains(mapping);
    }

    public static boolean isEmpty() {
        return MAPPINGS.isEmpty();
    }
}
