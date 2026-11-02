package dev.nytheria.compat.loader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import kotlin.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link ModJars} on the shapes the four mods have (design 2.0.0, B7, B14, B15). Plain unit test, no game classes.
 */
public class ModJarsTest {
    private static final String DEFAULT_AT = "META-INF/accesstransformer.cfg";

    @TempDir
    Path tempDir;

    private JarFile jar(String... names) throws IOException {
        Path path = Files.createTempFile(tempDir, "mod", ".jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(path))) {
            for (String name : names) {
                out.putNextEntry(new ZipEntry(name));
                out.write(name.getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return new JarFile(path.toFile());
    }

    private static List<byte[]> read(JarFile jar, List<String> declared, List<String> missing) {
        return ModJars.INSTANCE.readAccessTransformers(jar, declared, path -> {
            missing.add(path);
            return Unit.INSTANCE;
        });
    }

    // B15: Astral has a stale META-INF/mods.toml next to the real one
    @Test
    void readsOnlyTheNeoForgeModsToml() throws IOException {
        try (JarFile astral = jar("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
            var entry = ModJars.INSTANCE.modsToml(astral);
            assertNotNull(entry);
            assertEquals("META-INF/neoforge.mods.toml", entry.getName());
        }

        try (JarFile forgeOnly = jar("META-INF/mods.toml")) {
            assertNull(ModJars.INSTANCE.modsToml(forgeOnly));
        }
    }

    // B14: Astral and Curios have no [[accessTransformers]], the default path has to be read anyway
    @Test
    void readsTheDefaultAccessTransformerWhenNoneIsDeclared() throws IOException {
        var missing = new ArrayList<String>();
        try (JarFile curios = jar(DEFAULT_AT)) {
            var result = read(curios, List.of(), missing);
            assertEquals(1, result.size());
            assertArrayEquals(DEFAULT_AT.getBytes(StandardCharsets.UTF_8), result.get(0));
        }
        assertEquals(List.of(), missing);
    }

    // B7: Cataclysm and Lionfish API list the default path as well
    @Test
    void readsADeclaredDefaultPathOnce() throws IOException {
        var missing = new ArrayList<String>();
        try (JarFile cataclysm = jar(DEFAULT_AT, "META-INF/extra.cfg")) {
            assertEquals(2, read(cataclysm, List.of(DEFAULT_AT, "META-INF/extra.cfg"), missing).size());
        }
        assertEquals(List.of(), missing);
    }

    @Test
    void reportsOnlyADeclaredPathThatIsMissing() throws IOException {
        var missing = new ArrayList<String>();
        try (JarFile noTransformers = jar("META-INF/neoforge.mods.toml")) {
            assertEquals(0, read(noTransformers, List.of("META-INF/gone.cfg"), missing).size());
        }
        assertEquals(List.of("META-INF/gone.cfg"), missing);
    }

    // the lock reads what a jar nests, at any depth, though Nytheria Compat loads none of it
    @Test
    void findsNestedModsAtAnyDepth() throws IOException {
        byte[] toml = """
                [[mods]]
                modId="inner"
                [[dependencies.inner]]
                modId="minecraft"
                """.getBytes(StandardCharsets.UTF_8);
        byte[] inner = zip("META-INF/neoforge.mods.toml", toml);
        byte[] middle = zip("META-INF/jarjar/inner.jar", inner);
        Path path = Files.createTempFile(tempDir, "outer", ".jar");
        Files.write(path, zip("META-INF/jarjar/middle.jar", middle));

        try (JarFile outer = new JarFile(path.toFile())) {
            var found = ModJars.INSTANCE.nestedMods(outer, "outer.jar");
            assertEquals(1, found.size());
            assertEquals("inner", found.get(0).getModId());
            assertEquals("outer.jar!/META-INF/jarjar/middle.jar!/META-INF/jarjar/inner.jar", found.get(0).getJar());
        }
    }

    private static byte[] zip(String name, byte[] content) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            out.putNextEntry(new ZipEntry(name));
            out.write(content);
            out.closeEntry();
        }
        return bytes.toByteArray();
    }
}
