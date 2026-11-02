package dev.nytheria.compat.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link ModRootPathKeys}: the keys of spark's path to mod id map and the removal of repeated ones. Plain unit tests,
 * no game classes.
 */
public class ModRootPathKeysTest {
    @TempDir
    Path tempDir;

    private Path createJar(String name) throws IOException {
        Path jar = tempDir.resolve(name);

        try (OutputStream out = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("fabric.mod.json"));
            zip.write("{}".getBytes());
            zip.closeEntry();
        }

        return jar;
    }

    @Test
    public void zipRootKeyIsTheFileSystemString() throws IOException {
        Path jar = createJar("a.jar");

        try (FileSystem fs = FileSystems.newFileSystem(jar)) {
            Path root = fs.getPath("/");

            assertEquals(fs.toString(), ModRootPathKeys.keyOf(root));
            assertEquals(jar.toString(), ModRootPathKeys.keyOf(root));
        }
    }

    @Test
    public void zipPathBelowTheRootUsesTheNormalKey() throws IOException {
        Path jar = createJar("b.jar");

        try (FileSystem fs = FileSystems.newFileSystem(jar)) {
            Path inner = fs.getPath("/fabric.mod.json");

            assertEquals(inner.toAbsolutePath().normalize().toString(), ModRootPathKeys.keyOf(inner));
        }
    }

    @Test
    public void normalPathKeyIsAbsoluteAndNormalized() {
        Path folder = tempDir.resolve("mod");
        Path odd = tempDir.resolve("other").resolve("..").resolve("mod");

        assertEquals(folder.toAbsolutePath().normalize().toString(), ModRootPathKeys.keyOf(folder));
        assertEquals(ModRootPathKeys.keyOf(folder), ModRootPathKeys.keyOf(odd));
    }

    @Test
    public void duplicateZipRootIsDroppedAndTheFirstKept() throws IOException {
        Path jar = createJar("nytheria_compat.jar");

        // Two file systems for the same jar are not possible at the same time, so the second "container" gets the
        // same root path object, as Nytheria Compat's neoforge container does with the Nytheria Compat jar.
        try (FileSystem fs = FileSystems.newFileSystem(jar)) {
            Path root = fs.getPath("/");
            ModRootPathKeys keys = new ModRootPathKeys();

            List<Path> first = keys.removeSeen(List.of(root));
            List<Path> second = keys.removeSeen(List.of(root));

            assertEquals(1, first.size());
            assertSame(root, first.get(0));
            assertTrue(second.isEmpty());
        }
    }

    @Test
    public void duplicateNormalPathIsDroppedEvenWhenWrittenDifferently() {
        Path folder = tempDir.resolve("mod");
        Path odd = tempDir.resolve("x").resolve("..").resolve("mod");
        ModRootPathKeys keys = new ModRootPathKeys();

        List<Path> first = keys.removeSeen(List.of(folder));
        List<Path> second = keys.removeSeen(List.of(odd));

        assertEquals(List.of(folder), first);
        assertTrue(second.isEmpty());
    }

    @Test
    public void onlyTheRepeatedPathsOfAContainerAreDropped() throws IOException {
        Path jar = createJar("c.jar");
        Path other = tempDir.resolve("unique");

        try (FileSystem fs = FileSystems.newFileSystem(jar)) {
            Path root = fs.getPath("/");
            ModRootPathKeys keys = new ModRootPathKeys();

            keys.removeSeen(List.of(root));

            assertEquals(List.of(other), keys.removeSeen(List.of(root, other)));
        }
    }

    @Test
    public void differentJarsAreNotDuplicates() throws IOException {
        Path jarA = createJar("d.jar");
        Path jarB = createJar("e.jar");

        try (FileSystem fsA = FileSystems.newFileSystem(jarA); FileSystem fsB = FileSystems.newFileSystem(jarB)) {
            ModRootPathKeys keys = new ModRootPathKeys();

            assertEquals(1, keys.removeSeen(List.of(fsA.getPath("/"))).size());
            assertEquals(1, keys.removeSeen(List.of(fsB.getPath("/"))).size());
        }
    }

    @Test
    public void separateInstancesAreIndependent() {
        Path folder = tempDir.resolve("mod");

        assertEquals(List.of(folder), new ModRootPathKeys().removeSeen(List.of(folder)));
        assertEquals(List.of(folder), new ModRootPathKeys().removeSeen(List.of(folder)));
    }

    @Test
    public void theInputListIsNotChanged() {
        Path folder = tempDir.resolve("mod");
        List<Path> input = new java.util.ArrayList<>(List.of(folder, folder));

        new ModRootPathKeys().removeSeen(input);

        assertEquals(2, input.size());
    }
}
