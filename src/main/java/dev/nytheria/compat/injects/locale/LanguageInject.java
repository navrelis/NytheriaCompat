// TRACKED HASH: 803f79832294bb467746850ed2c5d03a86aa08e3
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.locale;

import com.google.common.collect.ImmutableMap;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.locale.LanguageInjection;

import java.util.*;

@Mixin(Language.class)
public abstract class LanguageInject implements LanguageInjection {

    @Mixin(targets = "net.minecraft.locale.Language$1")
    public abstract static class AnonymousLanguageInject implements LanguageInjection {
        @Shadow @Final Map<String, String> val$storage;

        @Override
        public Map<String, String> getLanguageData() {
            return this.val$storage;
        }

        @Override
        public @Nullable Component getComponent(String key) {
            return LanguageInjection.nytheriacompat$componentMap.get().get(key);
        }
    }

    @Override
    public Map<String, String> getLanguageData() {
        return ImmutableMap.of();
    }

    @Override
    public @Nullable Component getComponent(String key) {
        return null;
    }
}