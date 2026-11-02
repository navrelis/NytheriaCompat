package dev.nytheria.compat.loader.remap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import kotlin.collections.CollectionsKt;
import net.minecraftforge.srgutils.IMappingFile;
import org.junit.jupiter.api.Test;

/**
 * {@link ClassNameTable} has to give the same class names as the parsed mappings it stands in for on a full remap cache.
 */
public class ClassNameTableTest {
    private static final String RESOURCE = "/moj_intermediary.tiny";

    private static ClassNameTable table(String... lines) {
        return new ClassNameTable(CollectionsKt.asSequence(List.of(lines)));
    }

    @Test
    void mapsBothWaysAndIgnoresMembers() {
        var table = table(
            "tiny\t2\t0\tsearge\tintermediary",
            "c\ta/Outer\tnet/minecraft/class_1",
            "\tm\t()D\tgetTime\tmethod_1",
            "c\ta/Outer$Inner\tnet/minecraft/class_1$class_2"
        );

        assertEquals("net/minecraft/class_1", table.map("a/Outer"));
        assertEquals("a/Outer", table.unmap("net/minecraft/class_1"));
        assertEquals("net/minecraft/class_1$class_2", table.map("a/Outer$Inner"));
        assertEquals("a/Outer$Inner", table.unmap("net/minecraft/class_1$class_2"));
    }

    @Test
    void anUnmappedInnerClassFollowsItsOuterClass() {
        var table = table("c\ta/Outer\tnet/minecraft/class_1");

        assertEquals("net/minecraft/class_1$1", table.map("a/Outer$1"));
        assertEquals("net/minecraft/class_1$Lambda$2", table.map("a/Outer$Lambda$2"));
        assertEquals("a/Outer$1", table.unmap("net/minecraft/class_1$1"));
    }

    @Test
    void anUnknownNameStaysAsItIs() {
        var table = table("c\ta/Outer\tnet/minecraft/class_1");

        assertEquals("dev/nytheria/compat/Thing", table.map("dev/nytheria/compat/Thing"));
        assertEquals("dev/nytheria/compat/Thing$Inner", table.map("dev/nytheria/compat/Thing$Inner"));
        assertEquals("dev/nytheria/compat/Thing", table.unmap("dev/nytheria/compat/Thing"));
    }

    @Test
    void agreesWithTheParsedMappingsOnEveryClass() throws IOException {
        IMappingFile mappings;
        try (InputStream input = ClassNameTableTest.class.getResourceAsStream(RESOURCE)) {
            mappings = IMappingFile.load(input);
        }

        ClassNameTable table;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ClassNameTableTest.class.getResourceAsStream(RESOURCE), StandardCharsets.UTF_8))) {
            table = new ClassNameTable(CollectionsKt.asSequence(reader.lines().toList()));
        }

        assertTrue(mappings.getClasses().size() > 1000);
        for (var cls : mappings.getClasses()) {
            assertEquals(cls.getMapped(), table.map(cls.getOriginal()));
            assertEquals(cls.getOriginal(), table.unmap(cls.getMapped()));
            assertEquals(mappings.remapClass(cls.getOriginal() + "$Unmapped"), table.map(cls.getOriginal() + "$Unmapped"));
        }
    }
}
