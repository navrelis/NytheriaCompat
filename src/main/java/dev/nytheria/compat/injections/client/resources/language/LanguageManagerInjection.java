/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.resources.language;

import net.minecraft.client.resources.language.LanguageManager;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.Locale;

@FabricInjectedInterface(LanguageManager.class)
public interface LanguageManagerInjection {
    default Locale getJavaLocale() {
        throw NytheriaCompatHelper.createMixinException(LanguageManagerInjection.class, "getJavaLocale");
    }
}
