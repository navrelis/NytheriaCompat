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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Rule H6: the default of an extension method answers what vanilla answers, so a block, item, entity or fluid of a mod that
 * does not override it behaves like it does in plain Fabric. The defaults on the hot paths (light, friction, sound type,
 * explosion resistance, ladders, enchantment value, fluid resistance) are checked to call the vanilla method of the same
 * name and nothing else of NeoForge.
 * <p>
 * Reads the class files as bytes, like {@link IBlockExtensionDefaultsTest}, so no game class is loaded.
 */
public class ExtensionDefaultsTest {
    private static final String EXTENSIONS = "net/neoforged/neoforge/common/extensions/";

    private static ClassNode read(String internalName) throws IOException {
        try (InputStream stream = ExtensionDefaultsTest.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
            assertNotNull(stream, internalName + ".class is not on the test class path");
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    /** Every default {@code method} of {@code extension} calls {@code vanilla} on a Minecraft class and no NeoForge code but {@code self()}. */
    private static void assertDelegatesToVanilla(String extension, String method, String vanilla) throws IOException {
        List<MethodNode> defaults = new ArrayList<>();
        for (MethodNode candidate : read(EXTENSIONS + extension).methods) {
            if (candidate.name.equals(method) && candidate.instructions.size() > 0) defaults.add(candidate);
        }
        assertTrue(!defaults.isEmpty(), extension + "#" + method + " has no default");

        for (MethodNode node : defaults) {
            boolean callsVanilla = false;
            for (AbstractInsnNode insn : node.instructions) {
                if (!(insn instanceof MethodInsnNode call)) continue;
                if (call.owner.startsWith("net/minecraft/") && call.name.equals(vanilla)) callsVanilla = true;
                boolean neo = call.owner.startsWith("net/neoforged/") || call.owner.startsWith("dev/nytheria/compat/");
                assertTrue(!neo || call.name.equals("self") || call.name.startsWith("neo$"),
                        extension + "#" + method + node.desc + " calls " + call.owner + "#" + call.name + ", more than vanilla's " + vanilla);
            }
            assertTrue(callsVanilla, extension + "#" + method + node.desc + " does not call vanilla's " + vanilla);
        }
    }

    @Test
    public void blockDefaultsAreVanilla() throws IOException {
        assertDelegatesToVanilla("IBlockExtension", "getFriction", "getFriction");
        assertDelegatesToVanilla("IBlockExtension", "getLightEmission", "getLightEmission");
        assertDelegatesToVanilla("IBlockExtension", "getSoundType", "getSoundType");
        assertDelegatesToVanilla("IBlockExtension", "getExplosionResistance", "getExplosionResistance");
        assertDelegatesToVanilla("IBlockExtension", "isLadder", "is");
        assertDelegatesToVanilla("IBlockExtension", "isEmpty", "is");
    }

    @Test
    public void itemDefaultsAreVanilla() throws IOException {
        assertDelegatesToVanilla("IItemExtension", "getEnchantmentValue", "getEnchantmentValue");
        assertDelegatesToVanilla("IItemExtension", "getDefaultAttributeModifiers", "getDefaultAttributeModifiers");
    }

    @Test
    public void entityAndFluidDefaultsAreVanilla() throws IOException {
        assertDelegatesToVanilla("IEntityExtension", "getPickedResult", "getPickResult");
        assertDelegatesToVanilla("IFluidStateExtension", "getExplosionResistance", "getExplosionResistance");
    }

    @Test
    public void checkRejectsADefaultThatDoesMore() {
        // The helper must fail on a default that is not a delegation, or the tests above prove nothing.
        assertEquals(true, org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class,
                () -> assertDelegatesToVanilla("IBlockExtension", "getFriction", "getLightEmission")).getMessage().contains("does not call"));
    }
}
