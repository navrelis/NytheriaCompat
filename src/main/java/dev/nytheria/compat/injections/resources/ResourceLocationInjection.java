/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.resources;

import io.github.fabricators_of_create.porting_lib.extensions.common.ResourceLocationExtension;
import dev.nytheria.compat.processor.FabricInjectedInterface;

import net.minecraft.resources.ResourceLocation;

@FabricInjectedInterface(ResourceLocation.class)
public interface ResourceLocationInjection extends ResourceLocationExtension {
    private ResourceLocation self() { // Nytheria Compat: make private cuz otherwise things panic
        return (ResourceLocation) this;
    }

    default int compareNamespaced(ResourceLocation o) {
        var ret = self().getNamespace().compareTo(o.getNamespace());
        return ret != 0 ? ret : self().getPath().compareTo(o.getPath());
    }
}
