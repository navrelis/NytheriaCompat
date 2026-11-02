package dev.nytheria.compat.hook;

import dev.nytheria.compat.loader.ModLock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Set;

/**
 * The scope test of a hook: does this object belong to a NeoForge mod? A hook that answers no behaves like plain Fabric.
 *
 * <p>An object is a NeoForge object if its class was loaded from a NeoForge mod jar, or its registry id is in one of
 * {@link #NAMESPACES}. The second part covers a mod's blocks, items and entities of a vanilla class. Fabric mods with a
 * subclass of a NeoForge mod's class are not in scope: the class test looks at the object's own class only.
 *
 * <p>The answer for a class is cached, and the overloads allocate nothing, so they can sit on a hot path. They need the
 * game's registries; only {@link #isNeoNamespace} and {@link #isRemappedModJar} work without a running game.
 */
public final class Scope {
    private static final Set<String> NAMESPACES = ModLock.ALLOWED_NEOFORGE_MODS;
    /** {@code NytheriaCompatLoader} loads every NeoForge mod from its remapped copy in this folder. */
    private static final Path REMAPPED_MODS = Path.of(".nytheria_compat", "remappedMods");

    private static final ClassValue<Boolean> NEO_CLASS = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return isRemappedModJar(type.getProtectionDomain().getCodeSource());
        }
    };

    private Scope() {
    }

    /** Only the class of {@code object}. Use the overloads below for what has a registry id. */
    public static boolean isNeoObject(Object object) {
        return NEO_CLASS.get(object.getClass());
    }

    public static boolean isNeoObject(ResourceLocation id) {
        return isNeoNamespace(id.getNamespace());
    }

    public static boolean isNeoObject(Entity entity) {
        return NEO_CLASS.get(entity.getClass()) || isNeoObject(EntityType.getKey(entity.getType()));
    }

    public static boolean isNeoObject(Block block) {
        return NEO_CLASS.get(block.getClass()) || isNeoObject(BuiltInRegistries.BLOCK.getKey(block));
    }

    public static boolean isNeoObject(Item item) {
        return NEO_CLASS.get(item.getClass()) || isNeoObject(BuiltInRegistries.ITEM.getKey(item));
    }

    static boolean isNeoNamespace(String namespace) {
        return NAMESPACES.contains(namespace);
    }

    /** A config file of a NeoForge mod is named {@code <modid>-<type>.toml}; any other file is out of scope. */
    public static boolean isNeoConfigFile(Path file) {
        String name = file.getFileName().toString();
        int dash = name.indexOf('-');
        return dash > 0 && isNeoNamespace(name.substring(0, dash));
    }

    /** Classes without a code source (JDK classes, hidden and generated classes) and those outside a file are out of scope. */
    static boolean isRemappedModJar(CodeSource source) {
        if (source == null || source.getLocation() == null || !"file".equals(source.getLocation().getProtocol())) return false;
        try {
            Path folder = Path.of(source.getLocation().toURI()).getParent();
            return folder != null && folder.endsWith(REMAPPED_MODS);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
