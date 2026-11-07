package dev.nytheria.compat.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import dev.nytheria.compat.loader.ModLock.FabricMod;
import dev.nytheria.compat.loader.ModLock.FoundMod;
import org.junit.jupiter.api.Test;

/**
 * {@link ModLock}: what Nytheria Compat refuses at startup. Plain unit test, no game classes.
 */
public class ModLockTest {
    private static final List<FabricMod> CORE = mods("nytheriaquests", "tickmender");
    private static final List<FoundMod> FOUR = List.of(
            new FoundMod("cataclysm.jar", "cataclysm", false),
            new FoundMod("astral.jar", "astral_dimension", false),
            new FoundMod("lionfishapi.jar", "lionfishapi", false),
            new FoundMod("curios.jar", "curios", false));

    private static List<FabricMod> mods(String... ids) {
        return Arrays.stream(ids).map(id -> new FabricMod(id, List.of())).toList();
    }

    private static List<FoundMod> withFour(FoundMod extra) {
        var found = new ArrayList<>(FOUR);
        found.add(extra);
        return found;
    }

    @Test
    void theFourAlonePass() {
        assertEquals(List.of(), ModLock.INSTANCE.check(FOUR, CORE));
    }

    @Test
    void eachAllowedModMayBeMissing() {
        for (FoundMod missing : FOUR) {
            var found = new ArrayList<>(FOUR);
            found.remove(missing);
            assertEquals(List.of(), ModLock.INSTANCE.check(found, CORE), missing.getModId());
        }
    }

    @Test
    void anExtraNeoForgeModIsRefusedByJarAndId() {
        var problems = ModLock.INSTANCE.check(withFour(new FoundMod("extra-1.0.jar", "extra_mod", false)), CORE);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("extra-1.0.jar"), problems.get(0));
        assertTrue(problems.get(0).contains("extra_mod"), problems.get(0));
    }

    @Test
    void aNestedNeoForgeModIsRefused() {
        var nested = new FoundMod("curios.jar!/META-INF/jarjar/inner.jar", "inner_mod", false);
        var problems = ModLock.INSTANCE.check(withFour(nested), CORE);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("curios.jar!/META-INF/jarjar/inner.jar"), problems.get(0));
    }

    @Test
    void aMissingCoreModIsNamed() {
        var problems = ModLock.INSTANCE.check(FOUR, mods("nytheriaquests"));
        assertEquals(List.of("Required Fabric mod tickmender is missing."), problems);
    }

    @Test
    void theOldQuestBridgeIdAlsoPasses() {
        assertEquals(List.of(), ModLock.INSTANCE.check(FOUR, mods("questbridge", "tickmender")));
    }

    @Test
    void bothQuestIdsPass() {
        assertEquals(List.of(), ModLock.INSTANCE.check(FOUR, mods("nytheriaquests", "questbridge", "tickmender")));
    }

    @Test
    void neitherQuestIdNamesBoth() {
        var problems = ModLock.INSTANCE.check(FOUR, mods("tickmender"));
        assertEquals(List.of("Required Fabric mod nytheriaquests (or questbridge) is missing."), problems);
    }

    @Test
    void aDualLoaderJarThatFabricRunsIsNotRefused() {
        assertEquals(List.of(), ModLock.INSTANCE.check(withFour(new FoundMod("tide-fabric.jar", "tide", true)), CORE));
    }

    @Test
    void aModProvidingNytheriaQuestsSatisfiesTheQuestGroup() {
        var renamed = List.of(new FabricMod("nytheria_quests_next", List.of("nytheriaquests")), new FabricMod("tickmender", List.of()));
        assertEquals(List.of(), ModLock.INSTANCE.check(FOUR, renamed));
    }

    @Test
    void aModProvidingQuestBridgeSatisfiesTheQuestGroup() {
        var renamed = List.of(new FabricMod("nytheria_quests_next", List.of("questbridge")), new FabricMod("tickmender", List.of()));
        assertEquals(List.of(), ModLock.INSTANCE.check(FOUR, renamed));
    }

    @Test
    void aModProvidingTickMenderSatisfiesItsGroup() {
        var renamed = List.of(new FabricMod("nytheriaquests", List.of()), new FabricMod("tick_mender_next", List.of("tickmender")));
        assertEquals(List.of(), ModLock.INSTANCE.check(FOUR, renamed));
    }

    @Test
    void aModProvidingAnUnrelatedIdDoesNotSatisfyAGroup() {
        var mods = List.of(new FabricMod("nytheriaquests", List.of()), new FabricMod("tickmender_addon", List.of("foo")));
        assertEquals(List.of("Required Fabric mod tickmender is missing."), ModLock.INSTANCE.check(FOUR, mods));
    }

    @Test
    void bothCoreModsMissingNamesBoth() {
        var mods = List.of(new FabricMod("fabric-api", List.of("fabric")));
        assertEquals(List.of("Required Fabric mod nytheriaquests (or questbridge) is missing.", "Required Fabric mod tickmender is missing."),
                ModLock.INSTANCE.check(FOUR, mods));
    }

    @Test
    void theNeoForgeAllowListIsUnchanged() {
        assertEquals(Set.of("cataclysm", "astral_dimension", "lionfishapi", "curios"), ModLock.ALLOWED_NEOFORGE_MODS);
        assertEquals(List.of(), ModLock.INSTANCE.check(List.of(new FoundMod("curios.jar", "curios", false)), CORE));
        assertEquals(1, ModLock.INSTANCE.check(List.of(new FoundMod("x.jar", "x", false)), CORE).size());
        assertEquals(List.of(), ModLock.INSTANCE.check(List.of(new FoundMod("x.jar", "x", true)), CORE));
    }
}
