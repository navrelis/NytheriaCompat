package dev.nytheria.compat.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * {@code IBlockExtension} must not re-declare {@code FabricBlock#getAppearance}. Both declare it as a {@code default}
 * with the same descriptor, so a block class that also gets {@code FabricBlock} through another interface that overrides
 * it (FramedBlocks' {@code IFramedBlock}) ends up with two unrelated most-specific defaults, and the JVM throws
 * {@code IncompatibleClassChangeError: Conflicting default methods} on every call. Neo mods keep calling and overriding
 * {@code getAppearance} with the same name and descriptor, which resolves to the {@code FabricBlock} method instead.
 * <p>
 * Reads the class files as bytes (no class loading or linking, so no game classes are touched) and stays a plain unit
 * test.
 */
public class IBlockExtensionDefaultsTest {
    private static final String BLOCK_EXTENSION = "net/neoforged/neoforge/common/extensions/IBlockExtension";
    private static final String FABRIC_BLOCK = "net/fabricmc/fabric/api/block/v1/FabricBlock";
    private static final String GET_APPEARANCE = "getAppearance";

    private static ClassNode read(String internalName) throws IOException {
        try (InputStream stream = IBlockExtensionDefaultsTest.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
            assertNotNull(stream, internalName + ".class is not on the test class path");
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static List<MethodNode> named(ClassNode node, String name) {
        List<MethodNode> found = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if (method.name.equals(name)) {
                found.add(method);
            }
        }
        return found;
    }

    @Test
    public void fabricBlockDeclaresGetAppearanceAsDefault() throws IOException {
        // The premise of the fix: the method IBlockExtension used to repeat is inherited from here.
        List<MethodNode> methods = named(read(FABRIC_BLOCK), GET_APPEARANCE);
        assertEquals(1, methods.size(), "FabricBlock.getAppearance");
        MethodNode method = methods.get(0);
        assertTrue((method.access & Opcodes.ACC_ABSTRACT) == 0, "FabricBlock.getAppearance must be a default method");
        // (state, level, pos, side, queryState, queryPos): the six-argument form NeoForge calls.
        assertEquals(6, Type.getArgumentTypes(method.desc).length, "FabricBlock.getAppearance arguments");
    }

    @Test
    public void blockExtensionExtendsFabricBlock() throws IOException {
        assertTrue(read(BLOCK_EXTENSION).interfaces.contains(FABRIC_BLOCK),
                "IBlockExtension must keep inheriting getAppearance from FabricBlock");
    }

    @Test
    public void blockExtensionDoesNotRedeclareGetAppearance() throws IOException {
        List<MethodNode> methods = named(read(BLOCK_EXTENSION), GET_APPEARANCE);
        assertTrue(methods.isEmpty(),
                "IBlockExtension re-declares getAppearance (" + (methods.isEmpty() ? "" : methods.get(0).desc) + "): "
                        + "blocks that also implement an interface overriding FabricBlock#getAppearance would fail with "
                        + "IncompatibleClassChangeError (Conflicting default methods)");
    }
}
