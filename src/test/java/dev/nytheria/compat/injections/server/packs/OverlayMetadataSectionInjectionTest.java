package dev.nytheria.compat.injections.server.packs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.server.packs.AbstractPackResources;

/**
 * {@code neoforge:overlays} only apply to NeoForge sources (the packs of the NeoForge mods Nytheria Compat runs and Nytheria Compat's own
 * packs, see {@code NeoForgeSources}; first argument of the helper). Every other pack gets none, as on plain Fabric, with
 * or without {@code fabric:overlays}: Lithostitched 1.8.1 declares both lists, and the conditions of its NeoForge entries
 * are Fabric ones ({@code apollib:config}), which Nytheria Compat's condition codec turns into "true", so overlays the author
 * switched off by config (the debug suite with its armadillo spawns) were always applied.
 * <p>
 * A NeoForge source that also declares {@code fabric:overlays} gets its overlays from Fabric API.
 * <p>
 * Plain unit test: only the metadata section type and the pure helper are used, no game bootstrap.
 */
public class OverlayMetadataSectionInjectionTest {
    private static final List<String> NEO = List.of("overlay.breaks_seed_parity", "overlay.debug_suite");

    /** pack.mcmeta of Lithostitched 1.8.1. */
    private static final String LITHOSTITCHED = """
            {
              "pack": {"description": "Lithostitched resources", "pack_format": 48, "supported_formats": [48, 999], "min_format": 48, "max_format": 999},
              "neoforge:overlays": {"entries": [
                {"directory": "overlay.breaks_seed_parity", "formats": [48, 999], "min_format": 48, "max_format": 999,
                 "neoforge:conditions": [{"type": "apollib:config", "key": "lithostitched:breaks_seed_parity"}]},
                {"directory": "overlay.debug_suite", "formats": [48, 999], "min_format": 48, "max_format": 999,
                 "neoforge:conditions": [{"type": "apollib:config", "key": "lithostitched:debug_suite"}]}]},
              "fabric:overlays": {"entries": [
                {"directory": "overlay.breaks_seed_parity", "condition": {"condition": "apollib:config", "key": "lithostitched:breaks_seed_parity"}},
                {"directory": "overlay.debug_suite", "condition": {"condition": "apollib:config", "key": "lithostitched:debug_suite"}}]}
            }
            """;

    private static Object readFabricSection(String json) {
        return AbstractPackResources.getMetadataFromStream(OverlayMetadataSectionInjection.FABRIC_TYPE,
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void neoForgeSourceWithoutFabricOverlaysKeepsNeoForgeOverlays() {
        assertSame(NEO, OverlayMetadataSectionInjection.neoForgeOverlaysToAdd(true, false, NEO));
        assertEquals(NEO, OverlayMetadataSectionInjection.neoForgeOverlaysToAdd(true, false, NEO));
    }

    @Test
    void neoForgeSourceWithFabricOverlaysGetsNoNeoForgeOverlays() {
        assertEquals(List.of(), OverlayMetadataSectionInjection.neoForgeOverlaysToAdd(true, true, NEO));
    }

    @Test
    void otherPacksGetNoNeoForgeOverlaysWithOrWithoutFabricOverlays() {
        // Fabric mods (Lithostitched), datapacks, global packs: plain Fabric never applies neoforge:overlays
        assertEquals(List.of(), OverlayMetadataSectionInjection.neoForgeOverlaysToAdd(false, false, NEO));
        assertEquals(List.of(), OverlayMetadataSectionInjection.neoForgeOverlaysToAdd(false, true, NEO));
    }

    @Test
    void emptyListsStayEmpty() {
        assertEquals(List.of(), OverlayMetadataSectionInjection.neoForgeOverlaysToAdd(true, true, List.of()));
        assertEquals(List.of(), OverlayMetadataSectionInjection.neoForgeOverlaysToAdd(true, false, List.of()));
        assertEquals(List.of(), OverlayMetadataSectionInjection.neoForgeOverlaysToAdd(false, false, List.of()));
    }

    @Test
    void fabricSectionTypeUsesTheFabricSectionName() {
        assertEquals("fabric:overlays", OverlayMetadataSectionInjection.FABRIC_TYPE.getMetadataSectionName());
    }

    @Test
    void lithostitchedFabricSectionIsPresent() {
        assertNotNull(readFabricSection(LITHOSTITCHED));
    }

    @Test
    void anyJsonObjectInTheFabricSectionIsPresent() {
        assertNotNull(readFabricSection("{\"fabric:overlays\": {}}"));
        assertNotNull(readFabricSection("{\"fabric:overlays\": {\"entries\": []}}"));
        // malformed for Fabric API's own reader and a format Nytheria Compat has never seen: still counts as declared
        assertNotNull(readFabricSection("{\"fabric:overlays\": {\"entries\": \"nope\", \"future\": [1, {\"a\": null}]}}"));
        assertNotNull(readFabricSection("{\"fabric:overlays\": {\"entries\": [{\"directory\": 5}]}}"));
    }

    @Test
    void missingFabricSectionIsAbsent() {
        assertNull(readFabricSection("{\"pack\": {\"description\": \"x\", \"pack_format\": 48}}"));
        assertNull(readFabricSection("{\"neoforge:overlays\": {\"entries\": []}}"));
    }

    @Test
    void nonObjectFabricSectionIsAbsentBecauseVanillaOnlyHandsObjectsToTheCodec() {
        // AbstractPackResources.getMetadataFromStream calls GsonHelper.getAsJsonObject before the codec runs, so an array
        // or a string is rejected there (logged as an error) for every section type, Fabric API's own reader included.
        assertNull(readFabricSection("{\"fabric:overlays\": []}"));
        assertNull(readFabricSection("{\"fabric:overlays\": \"x\"}"));
    }
}
