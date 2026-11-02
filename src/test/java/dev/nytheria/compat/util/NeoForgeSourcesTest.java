package dev.nytheria.compat.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.RepositorySource;
import org.junit.jupiter.api.Test;

/**
 * The pure rules of {@link NeoForgeSources}, with the NeoForge mods of the NytheriaDevelopment pack as examples. Plain
 * unit test: no game bootstrap, no loaded mods.
 */
public class NeoForgeSourcesTest {
    /** What Nytheria Compat's mod list holds in that pack (the built-in neoforge mod included). */
    private static final Set<String> NEOFORGE_MODS = Set.of("neoforge", "astral_dimension", "cataclysm", "curios", "lionfishapi");
    private static final Set<String> ADDED_BY_NEOFORGE_MODS = Set.of("builtin/cataclysm_extra");

    private static boolean neo(String packId) {
        return NeoForgeSources.isNeoForgeSource(packId, NEOFORGE_MODS::contains, ADDED_BY_NEOFORGE_MODS::contains);
    }

    // ---------------------------------------------------------------------------------------------- which packs

    @Test
    void packsOfTheNeoForgeModsNytheriaCompatRunsAreNeoForgeSources() {
        assertTrue(neo("astral_dimension"));
        assertTrue(neo("cataclysm"));
        assertTrue(neo("curios"));
        assertTrue(neo("lionfishapi"));
        assertTrue(neo("mod/cataclysm"));
        assertTrue(neo("cataclysm:some_builtin_pack"));
        assertTrue(neo("mod/curios:x"));
    }

    @Test
    void packsANeoForgeModAddedThroughAddPackFindersEventAreNeoForgeSources() {
        assertTrue(neo("builtin/cataclysm_extra"));
        assertFalse(neo("builtin/aether_accessories"));
    }

    @Test
    void nytheriaCompatOwnPacksAreNeoForgeSources() {
        assertTrue(neo("nytheria_compat"));
        assertTrue(neo("neoforge"));
        assertTrue(neo("nytheria_compat:x"));
        assertTrue(neo("neoforge:x"));
    }

    @Test
    void fabricModsAreNotNeoForgeSources() {
        // mod packs, dual jars (they load as Fabric mods, so Nytheria Compat's list does not have them), Fabric API built-in
        // packs, runtime packs, NeoForge-style "mod/" ids of Fabric mods
        assertFalse(neo("dustydecorations"));
        assertFalse(neo("yungsextras"));
        assertFalse(neo("lithostitched"));
        assertFalse(neo("porting_lib_base"));
        assertFalse(neo("accessorify:1_21_1/dp"));
        assertFalse(neo("amendments:generated_server_pack"));
        assertFalse(neo("mod/yungsapi:locate_replacer"));
        assertFalse(neo("fabric"));
    }

    @Test
    void vanillaDatapacksAndUnknownPacksAreNotNeoForgeSources() {
        assertFalse(neo("vanilla"));
        assertFalse(neo("minecraft"));
        assertFalse(neo("file/nytheria_fixes.zip"));
        assertFalse(neo("file/cataclysm"));
        assertFalse(neo("global_packs/x"));
        assertFalse(neo("nytheria_worldgen_fixes"));
        assertFalse(neo("generated/fragmentum_layer"));
        assertFalse(neo("server"));
        assertFalse(neo(""));
        assertFalse(neo(null));
        assertFalse(neo(":cataclysm"));
    }

    // ---------------------------------------------------------------------------------------------- conditions

    private static JsonElement json(String s) {
        return JsonParser.parseString(s);
    }

    /** supplementaries' recipe/awnings/awning_black_2.json, shortened. */
    private static final String BOTH = """
            {"fabric:load_conditions": [{"condition": "supplementaries:flag", "flag": "awning"}],
             "neoforge:conditions": [{"type": "neoforge:not", "value": {"type": "supplementaries:flag", "flag": "flax"}}],
             "type": "minecraft:crafting_shaped", "result": {"id": "supplementaries:awning_black", "count": 2,
             "neoforge:conditions": "nested stays"}}
            """;

    @Test
    void fabricDecidesWhenAFileOutsideNeoForgeSourcesHasBothConditionKeys() {
        assertTrue(NeoForgeSources.leaveConditionsToFabric(false, json(BOTH)));
    }

    @Test
    void neoForgeConditionsAloneAreStillEvaluated() {
        // aether's data/supplementaries/recipe/copper_lantern_conversion.json: only neoforge:conditions (mod_loaded suppsquared)
        assertFalse(NeoForgeSources.leaveConditionsToFabric(false, json("{\"neoforge:conditions\": [{\"type\": \"neoforge:mod_loaded\", \"modid\": \"suppsquared\"}], \"type\": \"x\"}")));
        assertFalse(NeoForgeSources.leaveConditionsToFabric(false, json("{\"fabric:load_conditions\": [], \"type\": \"x\"}")));
        assertFalse(NeoForgeSources.leaveConditionsToFabric(false, json("{\"type\": \"x\"}")));
    }

    @Test
    void neoForgeSourcesKeepTheirConditions() {
        assertFalse(NeoForgeSources.leaveConditionsToFabric(true, json(BOTH)));
    }

    @Test
    void nonObjectsAreLeftAlone() {
        assertFalse(NeoForgeSources.leaveConditionsToFabric(false, json("[1, 2]")));
        assertFalse(NeoForgeSources.leaveConditionsToFabric(false, json("\"neoforge:conditions\"")));
        assertFalse(NeoForgeSources.leaveConditionsToFabric(false, null));
    }

    @Test
    void onlyTheRootNeoForgeConditionsAreRemovedAndTheInputStaysUnchanged() {
        JsonObject original = json(BOTH).getAsJsonObject();
        String before = original.toString();
        JsonObject stripped = NeoForgeSources.withoutNeoForgeConditions(original);

        assertNotSame(original, stripped);
        assertFalse(stripped.has("neoforge:conditions"));
        assertTrue(stripped.has("fabric:load_conditions"));
        assertEquals(List.of("fabric:load_conditions", "type", "result"), List.copyOf(stripped.keySet()));
        assertEquals("nested stays", stripped.getAsJsonObject("result").get("neoforge:conditions").getAsString());
        assertEquals(before, original.toString());
    }

    // ---------------------------------------------------------------------------------------------- layers / stacks

    private record Layer(String packId, boolean replace, List<String> entries) {
    }

    @Test
    void globalLootModifierLayersOfOtherPacksTakeNoPart() {
        // the 7 global_loot_modifiers.json layers of the pack (lowest priority first), plus a datapack and a NeoForge mod
        List<Layer> layers = List.of(
                new Layer("aether", false, List.of("aether:double_drops")),
                new Layer("artifacts", false, List.of()),
                new Layer("sophisticatedbackpacks", false, List.of()),
                new Layer("nytheria_compat", false, List.of()),
                new Layer("cataclysm", false, List.of("cataclysm:a")),
                new Layer("supplementaries", true, List.of("supplementaries:replace_rope")),
                new Layer("file/override", true, List.of("pack:x")),
                new Layer("twilightforest", false, List.of("twilightforest:fiery_pick_smelting")),
                new Layer("neoforge", false, List.of()),
                new Layer("astral_dimension", false, List.of("astral_dimension:b", "cataclysm:a")));
        NeoForgeSources.Ignored ignored = new NeoForgeSources.Ignored("NeoForge global loot modifier lists from packs that are not NeoForge mods");

        List<Layer> kept = NeoForgeSources.keepNeoForgeSources(layers, Layer::packId, p -> !neo(p), "neoforge:loot_modifiers/global_loot_modifiers.json", ignored);

        assertEquals(List.of("nytheria_compat", "cataclysm", "neoforge", "astral_dimension"), kept.stream().map(Layer::packId).toList());
        assertEquals(6, ignored.total());
        assertEquals("Ignored 6 NeoForge global loot modifier lists from packs that are not NeoForge mods: aether, artifacts, file/override, "
                + "sophisticatedbackpacks, supplementaries, twilightforest", ignored.message());

        // NeoForge's layering (LootModifierManager.prepare) over what is left: no "replace" of an ignored layer applies
        List<String> result = new ArrayList<>();
        for (Layer layer : kept) {
            if (layer.replace()) result.clear();
            for (String e : layer.entries()) {
                result.remove(e);
                result.add(e);
            }
        }
        assertEquals(List.of("astral_dimension:b", "cataclysm:a"), result);
    }

    @Test
    void keepNeoForgeSourcesReturnsTheSameListWhenNothingIsIgnored() {
        List<String> stack = List.of("astral_dimension", "neoforge", "nytheria_compat");
        assertSame(stack, NeoForgeSources.keepNeoForgeSources(stack, s -> s, p -> false, "f", null));
        assertEquals(List.of(), NeoForgeSources.keepNeoForgeSources(List.of("aether"), s -> s, p -> true, "f", null));
        assertEquals(List.of("b"), NeoForgeSources.keepNeoForgeSources(List.of("a", "b", "a"), s -> s, "a"::equals, "f", null));
    }

    @Test
    void ignoredCountsPerPack() {
        NeoForgeSources.Ignored ignored = new NeoForgeSources.Ignored("neoforge:biome_modifier entries from packs that are not NeoForge mods (plain Fabric ignores them)");
        assertEquals(0, ignored.total());
        ignored.add("yungsextras", "a");
        ignored.add("dustydecorations", "b");
        ignored.add("yungsextras", "c");
        ignored.add("betterdungeons", "d");
        assertEquals(4, ignored.total());
        assertEquals("Ignored 4 neoforge:biome_modifier entries from packs that are not NeoForge mods (plain Fabric ignores them): "
                + "betterdungeons, dustydecorations, yungsextras (2)", ignored.message());
    }

    @Test
    void modifierRegistries() {
        assertTrue(NeoForgeSources.isModifierRegistry(ResourceLocation.fromNamespaceAndPath("neoforge", "biome_modifier")));
        assertTrue(NeoForgeSources.isModifierRegistry(ResourceLocation.fromNamespaceAndPath("neoforge", "structure_modifier")));
        assertFalse(NeoForgeSources.isModifierRegistry(ResourceLocation.withDefaultNamespace("worldgen/biome")));
        assertFalse(NeoForgeSources.isModifierRegistry(ResourceLocation.fromNamespaceAndPath("lithostitched", "worldgen_modifier")));
    }

    // ---------------------------------------------------------------------------------------------- escape hatch, finders

    @Test
    void escapeHatchAppliesNeoForgeDataToEveryPack() {
        assertTrue(NeoForgeSources.appliesNeoForgeData("dustydecorations", true, NeoForgeSourcesTest::neo));
        assertTrue(NeoForgeSources.appliesNeoForgeData("file/nytheria_fixes.zip", true, NeoForgeSourcesTest::neo));
        assertFalse(NeoForgeSources.appliesNeoForgeData("dustydecorations", false, NeoForgeSourcesTest::neo));
        assertTrue(NeoForgeSources.appliesNeoForgeData("cataclysm", false, NeoForgeSourcesTest::neo));
    }

    @Test
    void everyPackCountsWhileANeoForgeModsPackFinderRunsAndTheFlagIsRestoredAfterwards() {
        List<Boolean> seen = new ArrayList<>();
        Predicate<String> none = p -> false;
        RepositorySource inner = acceptor -> seen.add(NeoForgeSources.appliesNeoForgeData("whatever_the_id", false, none));
        RepositorySource outer = acceptor -> {
            NeoForgeSources.recordingPacks(inner).loadPacks(acceptor);
            // still inside the outer finder after the nested one returned
            seen.add(NeoForgeSources.appliesNeoForgeData("whatever_the_id", false, none));
        };

        NeoForgeSources.recordingPacks(outer).loadPacks(pack -> { });
        assertEquals(List.of(true, true), seen);
        assertFalse(NeoForgeSources.appliesNeoForgeData("whatever_the_id", false, none));

        RepositorySource failing = acceptor -> {
            throw new IllegalStateException("finder failed");
        };
        assertThrows(IllegalStateException.class, () -> NeoForgeSources.recordingPacks(failing).loadPacks(pack -> { }));
        assertFalse(NeoForgeSources.appliesNeoForgeData("whatever_the_id", false, none));
    }

    // ---------------------------------------------------------------------------------------------- conditions per file

    @Test
    void scopeConditionsStripsAndCountsOnlyFilesWithBothKeysOutsideNeoForgeSources() {
        NeoForgeSources.Ignored ignored = new NeoForgeSources.Ignored(NeoForgeSources.CONDITIONS_WHAT.formatted("recipe"));
        List<String> asked = new ArrayList<>();
        Predicate<String> applies = p -> {
            asked.add(p);
            return neo(p);
        };

        JsonElement fabricMod = json(BOTH);
        JsonElement result = NeoForgeSources.scopeConditions("supplementaries", fabricMod, "supplementaries:awning_black_2", ignored, applies);
        assertNotSame(fabricMod, result);
        assertFalse(result.getAsJsonObject().has("neoforge:conditions"));
        assertTrue(result.getAsJsonObject().has("fabric:load_conditions"));

        JsonElement neoMod = json(BOTH);
        assertSame(neoMod, NeoForgeSources.scopeConditions("cataclysm", neoMod, "cataclysm:x", ignored, applies));

        // only neoforge:conditions (aether's copper_lantern_conversion): evaluated by Nytheria Compat, the source is not even asked
        JsonElement neoOnly = json("{\"neoforge:conditions\": [{\"type\": \"neoforge:mod_loaded\", \"modid\": \"suppsquared\"}], \"type\": \"x\"}");
        assertSame(neoOnly, NeoForgeSources.scopeConditions("aether", neoOnly, "supplementaries:copper_lantern_conversion", ignored, applies));
        JsonElement plain = json("{\"type\": \"x\"}");
        assertSame(plain, NeoForgeSources.scopeConditions("aether", plain, "aether:y", ignored, null));

        assertEquals(List.of("supplementaries", "cataclysm"), asked);
        assertEquals(1, ignored.total());
        assertEquals("Ignored 1 " + NeoForgeSources.CONDITIONS_WHAT.formatted("recipe") + ": supplementaries", ignored.message());
    }

    @Test
    void escapeHatchKeepsTheNeoForgeConditionsOfEveryFile() {
        JsonElement fabricMod = json(BOTH);
        assertSame(fabricMod, NeoForgeSources.scopeConditions("supplementaries", fabricMod, "f", null, p -> NeoForgeSources.appliesNeoForgeData(p, true, NeoForgeSourcesTest::neo)));
    }

    // ---------------------------------------------------------------------------------------------- registry load scope

    @Test
    void registryLoadScopesNestAndAreRestoredEvenWhenTheLoadThrows() {
        assertNull(NeoForgeSources.RegistryLoad.current());
        NeoForgeSources.RegistryLoad biomeModifiers = NeoForgeSources.RegistryLoad.begin(ResourceLocation.fromNamespaceAndPath("neoforge", "biome_modifier"));
        try {
            assertSame(biomeModifiers, NeoForgeSources.RegistryLoad.current());
            NeoForgeSources.RegistryLoad nested = NeoForgeSources.RegistryLoad.begin(ResourceLocation.withDefaultNamespace("dimension"));
            try {
                assertSame(nested, NeoForgeSources.RegistryLoad.current());
                throw new IllegalStateException("decoding failed");
            } catch (IllegalStateException expected) {
                // like the registry loader's error map
            } finally {
                nested.end();
            }
            assertSame(biomeModifiers, NeoForgeSources.RegistryLoad.current());
        } finally {
            biomeModifiers.end();
        }
        assertNull(NeoForgeSources.RegistryLoad.current());
    }
}
