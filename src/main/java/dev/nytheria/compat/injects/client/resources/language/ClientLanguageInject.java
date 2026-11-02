/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.resources.language;

import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.locale.LanguageInjection;

import java.util.List;
import java.util.Map;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageInject implements LanguageInjection {
    @Shadow @Final private Map<String, String> storage;
    @Shadow private static void appendFrom(String string, List<Resource> list, Map<String, String> map) {}

    @Unique private Map<String, Component> componentStorage = Map.of();

    private ClientLanguageInject(Map<String, String> storage, boolean defaultRightToLeft) {
    }

    @CreateInitializer
    private ClientLanguageInject(Map<String, String> storage, boolean defaultRightToLeft, Map<String, Component> componentStorage) {
        this(storage, defaultRightToLeft);
        this.componentStorage = componentStorage;
    }

    @CreateStatic
    private static void appendFrom(String languageName, List<Resource> resources, Map<String, String> destinationMap, Map<String, Component> componentMap) {
        LanguageInjection.nytheriacompat$componentOutput.set(componentMap::put);
        appendFrom(languageName, resources, destinationMap);
        LanguageInjection.nytheriacompat$componentOutput.remove();
    }

    @Override
    public Map<String, String> getLanguageData() {
        return storage;
    }

    @Override
    public @Nullable Component getComponent(String key) {
        return this.componentStorage.get(key);
    }
}
