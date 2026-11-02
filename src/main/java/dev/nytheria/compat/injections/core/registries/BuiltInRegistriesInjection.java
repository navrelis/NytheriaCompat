/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.core.registries;

import net.minecraft.resources.ResourceLocation;
import dev.nytheria.compat.mixin.core.BuiltInRegistriesAccessor;

import java.util.Collections;
import java.util.Set;

public interface BuiltInRegistriesInjection {
    static Set<ResourceLocation> getVanillaRegistrationOrder() {
        return Collections.unmodifiableSet(BuiltInRegistriesAccessor.getLoaders().keySet());
    }
}
