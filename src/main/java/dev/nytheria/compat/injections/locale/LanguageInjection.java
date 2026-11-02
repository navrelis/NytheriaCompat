/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.locale;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public interface LanguageInjection {
    ThreadLocal<Map<String, Component>> nytheriacompat$componentMap = ThreadLocal.withInitial(HashMap::new);
    ThreadLocal<BiConsumer<String, Component>> nytheriacompat$componentOutput = new ThreadLocal<>();

    static void loadFromJson(InputStream stream, BiConsumer<String, String> output, BiConsumer<String, Component> componentOutput) {
        nytheriacompat$componentOutput.set(componentOutput);
        Language.loadFromJson(stream, output);
        nytheriacompat$componentOutput.remove();
    }

    default Map<String, String> getLanguageData() {
        throw NytheriaCompatHelper.createMixinException(LanguageInjection.class, "getLanguageData");
    }

    default @Nullable Component getComponent(String key) {
        throw NytheriaCompatHelper.createMixinException(LanguageInjection.class, "getComponent");
    }
}
