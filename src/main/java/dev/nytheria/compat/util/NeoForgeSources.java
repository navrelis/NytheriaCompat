package dev.nytheria.compat.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.nytheria.compat.loader.NytheriaCompatFlags;
import dev.nytheria.compat.loader.NytheriaCompatLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Nytheria Compat: which packs are "NeoForge sources", whose NeoForge data Nytheria Compat's NeoForge systems handle, and the rules for
 * everything else.
 *
 * <p>Nytheria Compat exists to run NeoForge mods on Fabric and must not change anything else. It implements NeoForge's data
 * driven systems ({@code neoforge:overlays}, root {@code neoforge:conditions}, biome and structure modifiers, global
 * loot modifier lists, data maps), and plain Fabric has none of them. So they only apply to the packs of the NeoForge
 * mods Nytheria Compat runs; every other pack gets plain Fabric's semantics, where that data is inert.
 *
 * <p>A <b>NeoForge source</b> is
 * <ul>
 *     <li>the pack of a NeoForge mod Nytheria Compat runs (Nytheria Compat's own record of the NeoForge mods it created,
 *     {@code NytheriaCompatLoader.getMods()}, which includes NeoForge mods nested in NeoForge jars). Fabric API creates one
 *     pack per mod container with the mod id as pack id; a pack id {@code <id>:<name>} or {@code mod/<id>...} counts as
 *     the mod {@code <id>}'s,</li>
 *     <li>every pack a NeoForge mod adds through NeoForge's {@code AddPackFindersEvent} (recorded when it is added,
 *     see {@link #recordingPacks}),</li>
 *     <li>Nytheria Compat's built-in packs {@code nytheria_compat} and {@code neoforge} (and {@code nytheria_compat:...}/{@code neoforge:...}).</li>
 * </ul>
 * Everything else is not: Fabric mods (dual jars with a {@code neoforge.mods.toml} that load as Fabric mods and nested
 * JiJ Fabric mods included), Fabric API's {@code fabric} group pack, {@code vanilla}, world datapacks, global packs,
 * server and resource packs, and every pack id Nytheria Compat cannot attribute to a NeoForge mod.
 *
 * <p>The system property {@value #NEO_DATA_FOR_ALL_PACKS} ({@code -Dnytheriacompat.neoDataFromFabricMods=true}, see
 * {@link NytheriaCompatFlags}) restores the earlier behaviour: {@link #appliesNeoForgeData(String)} is then always true.
 *
 * <p>The rules ({@link #isNeoForgeSource(String, Predicate, Predicate)},
 * {@link #appliesNeoForgeData(String, boolean, Predicate)}, {@link #leaveConditionsToFabric},
 * {@link #scopeConditions(String, JsonElement, Object, Ignored, Predicate)}, {@link #keepNeoForgeSources}) take
 * everything they need as arguments and are unit tested; only {@link #isNeoForgeSource(String)} looks at the loaded
 * mods and only {@link #appliesNeoForgeData(String)} at the system property.
 */
public final class NeoForgeSources {
    public static final String NEO_DATA_FOR_ALL_PACKS = "nytheriacompat.neoDataFromFabricMods";
    /** The root key NeoForge reads conditions from ({@code ConditionalOps.DEFAULT_CONDITIONS_KEY}). */
    public static final String NEOFORGE_CONDITIONS = "neoforge:conditions";
    /** The root key Fabric API reads conditions from ({@code ResourceConditions.CONDITIONS_KEY}). */
    public static final String FABRIC_CONDITIONS = "fabric:load_conditions";

    /** Nytheria Compat's built-in packs: Nytheria Compat's own Fabric container and the built-in {@code neoforge} mod. */
    private static final Set<String> BUILTIN = Set.of("nytheria_compat", "neoforge");

    private static final Logger LOGGER = LoggerFactory.getLogger("Nytheria Compat/NeoForgeSources");
    private static final Set<String> OVERLAY_PACKS_LOGGED = ConcurrentHashMap.newKeySet();
    /** Ids of the packs NeoForge mods added through AddPackFindersEvent (only grows; pack ids, no data). */
    private static final Set<String> ADDED_BY_NEOFORGE_MODS = ConcurrentHashMap.newKeySet();
    /** True on this thread while a NeoForge mod's pack finder runs (see {@link #recordingPacks}). */
    private static final ThreadLocal<Boolean> IN_NEOFORGE_FINDER = ThreadLocal.withInitial(() -> false);
    private static volatile Set<String> neoForgeMods;

    private NeoForgeSources() {
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Which packs

    /**
     * Whether Nytheria Compat's NeoForge data systems handle the NeoForge data of this pack: it is a NeoForge source, or the
     * escape hatch {@value #NEO_DATA_FOR_ALL_PACKS} is on.
     */
    public static boolean appliesNeoForgeData(String packId) {
        return appliesNeoForgeData(packId, NytheriaCompatFlags.NEO_DATA_FROM_FABRIC_MODS, NeoForgeSources::isNeoForgeSource);
    }

    /**
     * The rule behind {@link #appliesNeoForgeData(String)}: the escape hatch is on ({@code forAllPacks}), a NeoForge
     * mod's pack finder is running on this thread (see {@link #recordingPacks}), or the pack is a NeoForge source.
     */
    public static boolean appliesNeoForgeData(String packId, boolean forAllPacks, Predicate<String> isNeoForgeSource) {
        return forAllPacks || IN_NEOFORGE_FINDER.get() || isNeoForgeSource.test(packId);
    }

    /** The opposite of {@link #appliesNeoForgeData}, for filters. */
    public static boolean ignoresNeoForgeData(String packId) {
        return !appliesNeoForgeData(packId);
    }

    /** Whether the pack {@code packId} is a NeoForge source, looked up in the loaded NeoForge mods (see the class comment). */
    public static boolean isNeoForgeSource(String packId) {
        Set<String> mods = neoForgeMods();
        return isNeoForgeSource(packId, mods::contains, ADDED_BY_NEOFORGE_MODS::contains);
    }

    /**
     * The rule behind {@link #isNeoForgeSource(String)}. The pack id itself, or (for {@code <id>:<name>} packs) the part
     * before the first {@code ':'}, after dropping a {@code mod/} prefix, names the owning mod.
     *
     * @param isNeoForgeMod          mod ids of the NeoForge mods Nytheria Compat runs
     * @param isAddedByNeoForgeMod   pack ids a NeoForge mod added through {@code AddPackFindersEvent}
     */
    public static boolean isNeoForgeSource(String packId, Predicate<String> isNeoForgeMod, Predicate<String> isAddedByNeoForgeMod) {
        if (packId == null || packId.isEmpty()) {
            return false;
        }

        if (isAddedByNeoForgeMod.test(packId)) {
            return true;
        }

        String id = packId.startsWith("mod/") ? packId.substring("mod/".length()) : packId;
        if (BUILTIN.contains(id) || isNeoForgeMod.test(id)) {
            return true;
        }

        int colon = id.indexOf(':');
        if (colon > 0) {
            String namespace = id.substring(0, colon);
            return BUILTIN.contains(namespace) || isNeoForgeMod.test(namespace);
        }

        return false;
    }

    /**
     * The mod ids of the NeoForge mods Nytheria Compat runs. Knit fills Nytheria Compat's mod list during Fabric's language adapter
     * stage ({@code KnitEarlierInitializer}, before any game class is loaded) and the list is final once Nytheria Compat has
     * sorted it in {@code finishModScanning}; packs are only read long after that. Only a list that contains the
     * built-in {@code neoforge} mod (created in the same step as every other NeoForge mod) is cached, so an early or
     * failed call is never kept for the session.
     */
    private static Set<String> neoForgeMods() {
        Set<String> mods = neoForgeMods;
        if (mods == null) {
            try {
                Set<String> ids = new HashSet<>();
                for (var mod : NytheriaCompatLoader.Companion.getInstance().getMods()) {
                    ids.add(mod.getModId());
                }

                mods = Set.copyOf(ids);
                if (!mods.contains("neoforge")) {
                    // Nytheria Compat's mods are not created yet: not cached, asked again later
                    LOGGER.debug("NeoForge mod list not complete yet: {}", new TreeSet<>(mods));
                    return mods;
                }

                neoForgeMods = mods; // the mod set never changes after start-up
                LOGGER.info("NeoForge sources: the packs of the NeoForge mods {}, the packs they add, nytheria_compat", new TreeSet<>(mods));
            } catch (Throwable t) {
                // Before Nytheria Compat's loader exists (or outside the game): not cached, asked again later.
                LOGGER.debug("NeoForge mod list not available yet", t);
                return Set.of();
            }
        }

        return mods;
    }

    /**
     * Wraps a pack finder a NeoForge mod registers through {@code AddPackFindersEvent}, so that every pack it adds (and
     * its children) counts as a NeoForge source whatever its id.
     */
    public static RepositorySource recordingPacks(RepositorySource finder) {
        return acceptor -> {
            // the finder reads its packs' metadata (overlays) before handing them over, so while it runs, every pack
            // counts as a NeoForge mod's pack; the flag is this thread's and is restored in finally
            boolean outer = IN_NEOFORGE_FINDER.get();
            IN_NEOFORGE_FINDER.set(true);
            try {
                finder.loadPacks(pack -> {
                    record(pack);
                    acceptor.accept(pack);
                });
            } finally {
                if (!outer) {
                    IN_NEOFORGE_FINDER.remove();
                }
            }
        };
    }

    private static void record(Pack pack) {
        if (ADDED_BY_NEOFORGE_MODS.add(pack.getId())) {
            LOGGER.debug("Pack {} was added by a NeoForge mod", pack.getId());
        }

        for (Pack child : pack.getChildren()) {
            record(child);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The rules

    /**
     * Whether Nytheria Compat leaves the root {@code neoforge:conditions} of this file to Fabric API: the file is not from a
     * NeoForge source and also carries {@code fabric:load_conditions}, which Fabric API evaluates (as on plain Fabric).
     * A file with only {@code neoforge:conditions} keeps them (a recipe for an absent mod stays out).
     */
    public static boolean leaveConditionsToFabric(boolean neoForgeSource, JsonElement json) {
        return !neoForgeSource && json != null && json.isJsonObject()
                && json.getAsJsonObject().has(NEOFORGE_CONDITIONS) && json.getAsJsonObject().has(FABRIC_CONDITIONS);
    }

    /** A shallow copy of {@code json} without its root {@code neoforge:conditions}; {@code json} is not changed. */
    public static JsonObject withoutNeoForgeConditions(JsonObject json) {
        JsonObject copy = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getKey().equals(NEOFORGE_CONDITIONS)) {
                copy.add(entry.getKey(), entry.getValue());
            }
        }

        return copy;
    }

    /**
     * {@code json} as Nytheria Compat's NeoForge condition handling should see it: without its root {@code neoforge:conditions}
     * when {@link #leaveConditionsToFabric} says so (counted in {@code ignored}, may be null), else unchanged.
     */
    public static JsonElement scopeConditions(String packId, JsonElement json, Object fileId, Ignored ignored) {
        return scopeConditions(packId, json, fileId, ignored, NeoForgeSources::appliesNeoForgeData);
    }

    /**
     * The rule behind {@link #scopeConditions(String, JsonElement, Object, Ignored)}; {@code appliesNeoForgeData} is
     * only asked for files that carry both keys.
     */
    public static JsonElement scopeConditions(String packId, JsonElement json, Object fileId, Ignored ignored, Predicate<String> appliesNeoForgeData) {
        if (json != null && json.isJsonObject() && json.getAsJsonObject().has(NEOFORGE_CONDITIONS)
                && json.getAsJsonObject().has(FABRIC_CONDITIONS) && leaveConditionsToFabric(appliesNeoForgeData.test(packId), json)) {
            if (ignored != null) {
                ignored.add(packId, fileId);
            }

            return withoutNeoForgeConditions(json.getAsJsonObject());
        }

        return json;
    }

    /**
     * The items of {@code items} whose pack is not ignored, in order; the ignored ones are counted in {@code ignored}
     * (may be null). Returns {@code items} itself when nothing is ignored.
     */
    public static <T> List<T> keepNeoForgeSources(List<T> items, Function<T, String> packIdOf, Predicate<String> ignorePack, Object fileId, Ignored ignored) {
        List<T> kept = null;
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            String packId = packIdOf.apply(item);
            if (ignorePack.test(packId)) {
                if (kept == null) {
                    kept = new ArrayList<>(items.subList(0, i));
                }

                if (ignored != null) {
                    ignored.add(packId, fileId);
                }
            } else if (kept != null) {
                kept.add(item);
            }
        }

        return kept == null ? items : kept;
    }

    /** The NeoForge datapack registries whose entries are only loaded from NeoForge sources. */
    public static final Set<ResourceLocation> MODIFIER_REGISTRIES = Set.of(
            ResourceLocation.fromNamespaceAndPath("neoforge", "biome_modifier"),
            ResourceLocation.fromNamespaceAndPath("neoforge", "structure_modifier"));

    /** Whether the datapack registry {@code registryId} holds biome or structure modifiers. */
    public static boolean isModifierRegistry(ResourceLocation registryId) {
        return MODIFIER_REGISTRIES.contains(registryId);
    }

    /**
     * The counters of one datapack registry's load ({@code RegistryDataLoader.loadContentsFromManager}). The vanilla
     * code between that method and the per-file method has no room for an argument, so the current load is kept in a
     * thread local that {@link #begin} sets and {@link #end} restores (callers do that in {@code finally}). Registries
     * load one after another on one thread; a file loaded outside such a call (the client resolving synced registry
     * entries from its own packs) is still decided, only not counted.
     */
    public static final class RegistryLoad {
        private static final ThreadLocal<RegistryLoad> CURRENT = new ThreadLocal<>();

        private final RegistryLoad previous;
        private final Ignored modifiers;
        private final Ignored conditions;

        private RegistryLoad(ResourceLocation registryId, RegistryLoad previous) {
            this.previous = previous;
            this.modifiers = new Ignored(registryId + " entries from packs that are not NeoForge mods (plain Fabric ignores them)");
            this.conditions = new Ignored(CONDITIONS_WHAT.formatted(registryId));
        }

        public static RegistryLoad begin(ResourceLocation registryId) {
            RegistryLoad load = new RegistryLoad(registryId, CURRENT.get());
            CURRENT.set(load);
            return load;
        }

        /** Restores the previous scope and logs what this load ignored (one INFO line per kind, only if any). */
        public void end() {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }

            modifiers.log();
            conditions.log();
        }

        /** The load running on this thread, null outside one (for tests). */
        static RegistryLoad current() {
            return CURRENT.get();
        }

        public static void ignoredModifier(String packId, Object fileId) {
            RegistryLoad load = CURRENT.get();
            if (load != null) {
                load.modifiers.add(packId, fileId);
            } else {
                LOGGER.debug("Ignored {} from pack {}", fileId, packId);
            }
        }

        public static JsonElement scopeConditions(String packId, JsonElement json, Object fileId) {
            RegistryLoad load = CURRENT.get();
            return NeoForgeSources.scopeConditions(packId, json, fileId, load == null ? null : load.conditions);
        }
    }

    /** The log text for ignored conditions; {@code %s} is the data directory or registry. */
    public static final String CONDITIONS_WHAT = "neoforge:conditions in '%s' files that also carry fabric:load_conditions and are not from NeoForge mods (Fabric API decides, as on plain Fabric)";

    /**
     * Logs once per pack (at INFO, later at DEBUG) that the {@code neoforge:overlays} of a pack were not applied. Pack
     * metadata is read again on every pack scan, so a per-scan line would repeat on every reload.
     */
    public static void logIgnoredOverlays(String packId, List<String> overlays) {
        if (OVERLAY_PACKS_LOGGED.add(packId)) {
            LOGGER.info("Ignored the NeoForge overlays {} of pack {}, which is not a NeoForge mod (plain Fabric ignores them) [-D{}=true applies them]", overlays, packId, NEO_DATA_FOR_ALL_PACKS);
        } else {
            LOGGER.debug("Ignored the NeoForge overlays {} of pack {}", overlays, packId);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Logging

    /**
     * Counts the files one system ignored in one load, per pack, and logs them as one INFO line ({@link #log}); every
     * file is logged at DEBUG. Not thread safe: one instance belongs to one load on one thread.
     */
    public static final class Ignored {
        private final String what;
        private final Map<String, Integer> perPack = new TreeMap<>();
        private int total;

        /** @param what completes "Ignored N ...", e.g. "NeoForge global loot modifier lists from packs that are not NeoForge mods" */
        public Ignored(String what) {
            this.what = what;
        }

        public void add(String packId, Object fileId) {
            total++;
            perPack.merge(packId, 1, Integer::sum);
            LOGGER.debug("Ignored {} from pack {}: {}", fileId, packId, what);
        }

        public int total() {
            return total;
        }

        public String message() {
            StringBuilder sb = new StringBuilder("Ignored ").append(total).append(' ').append(what).append(':');
            perPack.forEach((pack, n) -> sb.append(' ').append(pack).append(n > 1 ? " (" + n + ")" : "").append(','));
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }

        /** One INFO line if anything was ignored. */
        public void log() {
            if (total > 0) {
                LOGGER.info("{} [-D{}=true restores them]", message(), NEO_DATA_FOR_ALL_PACKS);
            }
        }
    }
}
