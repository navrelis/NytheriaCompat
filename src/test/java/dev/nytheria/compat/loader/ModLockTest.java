package dev.nytheria.compat.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import dev.nytheria.compat.loader.ModLock.FoundMod;
import org.junit.jupiter.api.Test;

/**
 * {@link ModLock}: what Nytheria Compat refuses at startup. Plain unit test, no game classes.
 */
public class ModLockTest {
    private static final Set<String> CORE = Set.of("questbridge", "tickmender");
    private static final List<FoundMod> FOUR = List.of(
            new FoundMod("cataclysm.jar", "cataclysm", false),
            new FoundMod("astral.jar", "astral_dimension", false),
            new FoundMod("lionfishapi.jar", "lionfishapi", false),
            new FoundMod("curios.jar", "curios", false));

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
        var problems = ModLock.INSTANCE.check(FOUR, Set.of("questbridge"));
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("tickmender"), problems.get(0));
    }

    @Test
    void aDualLoaderJarThatFabricRunsIsNotRefused() {
        assertEquals(List.of(), ModLock.INSTANCE.check(withFour(new FoundMod("tide-fabric.jar", "tide", true)), CORE));
    }
}
