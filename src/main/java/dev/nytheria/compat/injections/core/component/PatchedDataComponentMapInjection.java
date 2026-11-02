/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.core.component;

import net.minecraft.core.component.PatchedDataComponentMap;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@FabricInjectedInterface(PatchedDataComponentMap.class)
public interface PatchedDataComponentMapInjection {
    default boolean isPatchEmpty() {
        throw NytheriaCompatHelper.createMixinException(PatchedDataComponentMapInjection.class, "isPatchEmpty");
    }
}
