// TRACKED HASH: 66608c9c6edf5ebf79f290052253701c1e738d8e
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.neoforge.client.extensions.IKeyMappingExtension;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyMappingLookup;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.hook.NeoKeyMappings;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(KeyMapping.class)
public abstract class KeyMappingInject implements IKeyMappingExtension {
    @Shadow private InputConstants.Key key;
    @Shadow @Final private static Map<InputConstants.Key, KeyMapping> MAP;
    @Unique private static final KeyMappingLookup FORGE_MAP = new KeyMappingLookup();
    @Mutable @Shadow @Final private String name;
    @Mutable @Shadow @Final private InputConstants.Key defaultKey;
    @Mutable @Shadow @Final private String category;
    @Shadow @Final private static Map<String, KeyMapping> ALL;
    @Shadow @Final private static Set<String> CATEGORIES;
    @Shadow @Final private static Map<String, Integer> CATEGORY_SORT_ORDER;
    @Unique private KeyModifier keyModifierDefault = KeyModifier.NONE;
    @Unique private KeyModifier keyModifier = KeyModifier.NONE;
    @Unique private IKeyConflictContext keyConflictContext = KeyConflictContext.UNIVERSAL;

    @CreateInitializer
    public KeyMappingInject(String description, IKeyConflictContext keyConflictContext, final InputConstants.Type inputType, final int keyCode, String category) {
        this(description, keyConflictContext, inputType.getOrCreate(keyCode), category);
    }

    @CreateInitializer
    public KeyMappingInject(String description, IKeyConflictContext keyConflictContext, InputConstants.Key keyCode, String category) {
        this(description, keyConflictContext, KeyModifier.NONE, keyCode, category);
    }

    @CreateInitializer
    public KeyMappingInject(String description, IKeyConflictContext keyConflictContext, KeyModifier keyModifier, final InputConstants.Type inputType, final int keyCode, String category) {
        this(description, keyConflictContext, keyModifier, inputType.getOrCreate(keyCode), category);
    }

    @CreateInitializer
    public KeyMappingInject(String description, IKeyConflictContext keyConflictContext, KeyModifier keyModifier, InputConstants.Key keyCode, String category) {
        this.name = description;
        this.key = keyCode;
        this.defaultKey = keyCode;
        this.category = category;
        this.keyConflictContext = keyConflictContext;
        this.keyModifier = keyModifier;
        this.keyModifierDefault = keyModifier;
        if (this.keyModifier.matches(keyCode))
            this.keyModifier = KeyModifier.NONE;

        NeoKeyMappings.add((KeyMapping) (Object) this);
        ALL.put(description, (KeyMapping) (Object) this);
        FORGE_MAP.put(keyCode, (KeyMapping) (Object) this);
        MAP.put(keyCode, (KeyMapping) (Object) this);
        CATEGORIES.add(category);
    }

    // Nytheria Compat: vanilla's compareTo throws a NullPointerException when a category is not in CATEGORY_SORT_ORDER (a mod added it
    //  through the NeoForge constructors). Vanilla runs for everything else, so the key menu of other mods is unchanged.
    @WrapMethod(method = "compareTo(Lnet/minecraft/client/KeyMapping;)I")
    private int nytheriacompat$compareUnknownCategories(KeyMapping mapping, Operation<Integer> original) {
        Integer tCat = CATEGORY_SORT_ORDER.get(this.category);
        Integer oCat = CATEGORY_SORT_ORDER.get(mapping.getCategory());
        if (this.category.equals(mapping.getCategory()) || (tCat != null && oCat != null)) return original.call(mapping);
        if (tCat == null && oCat != null) return 1;
        if (tCat != null) return -1;
        return I18n.get(this.category).compareTo(I18n.get(mapping.getCategory()));
    }

    @Inject(method = "resetMapping", at = @At("HEAD"))
    private static void nytheriacompat$resetForgeMap(CallbackInfo ci) {
        FORGE_MAP.clear();
    }

    @Inject(method = "resetMapping", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static void nytheriacompat$registerMappingToForgeMap(CallbackInfo ci, @Local KeyMapping mapping) {
        FORGE_MAP.put(mapping.getKey(), mapping);
    }

    // Nytheria Compat: NeoForge fires every mapping of a key. Only the mappings of NeoForge mods do here; the one mapping vanilla
    //  holds for the key (a Fabric mod's, last registered wins) still fires once, so a key without a NeoForge mapping is vanilla's.
    @Unique
    private static List<KeyMapping> nytheriacompat$neoMappings(InputConstants.Key key, boolean releasing) {
        if (NeoKeyMappings.isEmpty()) return List.of();
        List<KeyMapping> mappings = FORGE_MAP.getAll(key, releasing);
        mappings.removeIf(mapping -> !NeoKeyMappings.contains(mapping));
        return mappings;
    }

    @WrapMethod(method = "click")
    private static void nytheriacompat$wrapKeyClick(InputConstants.Key key, Operation<Void> original, @Share("currentMap") LocalRef<KeyMapping> currentMap) {
        List<KeyMapping> neoMappings = nytheriacompat$neoMappings(key, false);
        for (KeyMapping keyMapping : neoMappings) {
            currentMap.set(keyMapping);
            original.call(key);
        }
        currentMap.set(null);
        KeyMapping vanilla = MAP.get(key);
        if (neoMappings.isEmpty() || (vanilla != null && !neoMappings.contains(vanilla))) {
            original.call(key);
        }
    }

    @WrapOperation(method = "click", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static <K, V> V nytheriacompat$useForgeMappingIfPossibleForClick(Map<K, V> instance, K o, Operation<V> original, @Share("currentMap") LocalRef<KeyMapping> currentMap) {
        if (currentMap.get() != null) {
            return (V) currentMap.get();
        }

        //noinspection MixinExtrasOperationParameters
        return original.call(instance, o);
    }

    @WrapMethod(method = "set")
    private static void nytheriacompat$wrapKeySet(InputConstants.Key key, boolean held, Operation<Void> original, @Share("currentMap") LocalRef<KeyMapping> currentMap) {
        List<KeyMapping> neoMappings = nytheriacompat$neoMappings(key, !held);
        for (KeyMapping keyMapping : neoMappings) {
            currentMap.set(keyMapping);
            original.call(key, held);
        }
        currentMap.set(null);
        KeyMapping vanilla = MAP.get(key);
        if (neoMappings.isEmpty() || (vanilla != null && !neoMappings.contains(vanilla))) {
            original.call(key, held);
        }
    }

    @WrapOperation(method = "set", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static <K, V> V nytheriacompat$useForgeMappingIfPossible(Map<K, V> instance, K o, Operation<V> original, @Share("currentMap") LocalRef<KeyMapping> currentMap) {
        if (currentMap.get() != null) {
            return (V) currentMap.get();
        }

        //noinspection MixinExtrasOperationParameters
        return original.call(instance, o);
    }

    // Nytheria Compat-exclusive injects
    @Inject(method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILjava/lang/String;)V", at = @At("TAIL"))
    private void nytheriacompat$addKeyValueInForgeMap(String name, InputConstants.Type type, int keyCode, String category, CallbackInfo ci) {
        FORGE_MAP.put(this.key, (KeyMapping) (Object) this);
    }

    @NotNull
    @Override
    public InputConstants.Key getKey() {
        return this.key;
    }

    @Override
    public void setKeyConflictContext(IKeyConflictContext keyConflictContext) {
        this.keyConflictContext = keyConflictContext;
    }

    @Override
    public IKeyConflictContext getKeyConflictContext() {
        if (keyConflictContext == null)
            keyConflictContext = KeyConflictContext.UNIVERSAL;

        return keyConflictContext;
    }

    @Override
    public KeyModifier getDefaultKeyModifier() {
        if (keyModifierDefault == null)
            keyModifierDefault = KeyModifier.NONE;

        return this.keyModifierDefault;
    }

    @Override
    public KeyModifier getKeyModifier() {
        if (keyModifier == null)
            keyModifier = KeyModifier.NONE;

        return keyModifier;
    }

    @Override
    public void setKeyModifierAndCode(KeyModifier keyModifier, InputConstants.Key keyCode) {
        MAP.remove(this.key);
        FORGE_MAP.remove((KeyMapping) (Object) this);

        if (keyModifier == null || keyModifier.matches(keyCode))
            keyModifier = KeyModifier.NONE;

        this.key = keyCode;
        this.keyModifier = keyModifier;

        MAP.put(keyCode, (KeyMapping) (Object) this);
        FORGE_MAP.put(keyCode, (KeyMapping) (Object) this);
    }
}
