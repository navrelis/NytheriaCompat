/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.data.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import dev.nytheria.compat.injections.core.RegistrySetBuilderInjection;
import dev.nytheria.compat.mixin.data.registries.VanillaRegistriesAccessor;

import java.util.List;

public interface VanillaRegistriesInjection {
    List<? extends ResourceKey<? extends Registry<?>>> DATAPACK_REGISTRY_KEYS = ((RegistrySetBuilderInjection) VanillaRegistriesAccessor.getBuilder()).getEntryKeys();
}
