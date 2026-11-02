package dev.nytheria.compat.hook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * {@link Scope}'s two tests. These are plain unit tests: the overloads for entities, blocks and items need the game's
 * registries, the class test and the namespace test below are what they are made of.
 */
public class ScopeTest {
    private static CodeSource at(Path path) throws MalformedURLException {
        return new CodeSource(path.toUri().toURL(), (Certificate[]) null);
    }

    /** Defines a class with {@code jar} as its code source, like Fabric Loader does. */
    private static final class JarLoader extends ClassLoader {
        Object instanceFrom(Path jar) throws Exception {
            ClassWriter writer = new ClassWriter(0);
            writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "Plain", null, "java/lang/Object", null);
            var constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            constructor.visitCode();
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitMaxs(1, 1);
            constructor.visitEnd();
            writer.visitEnd();
            byte[] bytes = writer.toByteArray();
            return defineClass("Plain", bytes, 0, bytes.length, new ProtectionDomain(at(jar), null)).getConstructor().newInstance();
        }
    }

    @Test
    public void classFromRemappedModsIsNeo() throws Exception {
        assertTrue(Scope.isNeoObject(new JarLoader().instanceFrom(Path.of("game/.nytheria_compat/remappedMods/cataclysm_255_abc.jar"))));
    }

    @Test
    public void classFromAnotherJarIsNot() throws Exception {
        assertFalse(Scope.isNeoObject(new JarLoader().instanceFrom(Path.of("game/mods/fabric-mod.jar"))));
    }

    @Test
    public void jarElsewhereInTheCacheIsNot() throws Exception {
        assertFalse(Scope.isNeoObject(new JarLoader().instanceFrom(Path.of("game/.nytheria_compat/minecraft_1.21.1-mojang_10.jar"))));
    }

    @Test
    public void folderOfTheSameNameOutsideTheCacheIsNot() throws Exception {
        assertFalse(Scope.isRemappedModJar(at(Path.of("game/remappedMods/a.jar"))));
    }

    @Test
    public void classesOfTheRunningJvmAreNot() {
        assertFalse(Scope.isNeoObject("a JDK class has no code source"));
        assertFalse(Scope.isNeoObject(new ScopeTest()));
        assertFalse(Scope.isNeoObject((Object) ResourceLocation.fromNamespaceAndPath("cataclysm", "a")));
        assertFalse(Scope.isRemappedModJar(null));
    }

    @Test
    public void theFourNamespacesAreNeo() {
        for (String namespace : new String[] {"cataclysm", "astral_dimension", "lionfishapi", "curios"}) {
            assertTrue(Scope.isNeoObject(ResourceLocation.fromNamespaceAndPath(namespace, "path")), namespace);
        }
    }

    @Test
    public void configFilesOfTheFourModsAreNeo() {
        for (String name : new String[] {"cataclysm-common.toml", "curios-server.toml", "lionfishapi-client.toml", "astral_dimension-common.toml"}) {
            assertTrue(Scope.isNeoConfigFile(java.nio.file.Path.of("config", name)), name);
        }
    }

    @Test
    public void otherConfigFilesAreNot() {
        for (String name : new String[] {"quark-common.toml", "cataclysm.toml", "curios_extra-common.toml", "-common.toml"}) {
            assertFalse(Scope.isNeoConfigFile(java.nio.file.Path.of("config", name)), name);
        }
    }

    @Test
    public void otherNamespacesAreNot() {
        for (String namespace : new String[] {"minecraft", "neoforge", "fabric", "nytheria_compat", "geckolib", "cataclysm_addon"}) {
            assertFalse(Scope.isNeoObject(ResourceLocation.fromNamespaceAndPath(namespace, "path")), namespace);
        }
    }

    @Test
    public void thePathOfAnIdDoesNotCount() {
        assertFalse(Scope.isNeoObject(ResourceLocation.fromNamespaceAndPath("minecraft", "cataclysm")));
    }
}
