package dev.nytheria.compat.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nytheria Compat: the map keys that the spark profiler builds from the root paths of all Fabric mods, and a filter that
 * removes the paths whose key has already been seen.
 *
 * <p>spark ({@code FabricClassSourceLookup#constructPathToModIdMap}) puts every root path of every mod container into
 * a Guava {@code ImmutableMap}, whose builder throws on a repeated key. Nytheria Compat has the extra container
 * {@code neoforge}, whose root path is the root of the Nytheria Compat jar, so it has the same key as Nytheria Compat itself. This
 * class computes the key exactly as spark does, so that only true duplicates are dropped.
 *
 * <p>spark's key is: the file system's {@code toString()} (the path of the jar) for the root {@code /} of a jar file
 * system, and otherwise {@code path.toAbsolutePath().normalize().toString()}.
 *
 * <p>One instance is meant for one map construction: the first path with a key keeps it, later paths with the same key
 * are dropped. No game classes, so it can be unit tested.
 */
public final class ModRootPathKeys {
    private final Set<String> seen = new HashSet<>();

    /** The key spark uses for {@code path} in its path to mod id map. */
    public static String keyOf(Path path) {
        if ("jar".equals(path.toUri().getScheme()) && "/".equals(path.toString())) {
            return path.getFileSystem().toString();
        }

        return path.toAbsolutePath().normalize().toString();
    }

    /**
     * The paths of {@code paths} whose key has not been seen before, in order; their keys count as seen from now on.
     * {@code paths} is not changed.
     */
    public List<Path> removeSeen(List<Path> paths) {
        List<Path> result = new ArrayList<>(paths.size());

        for (Path path : paths) {
            if (seen.add(keyOf(path))) {
                result.add(path);
            }
        }

        return result;
    }
}
