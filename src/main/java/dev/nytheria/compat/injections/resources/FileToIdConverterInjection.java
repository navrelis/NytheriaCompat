/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.resources;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.Map;

public interface FileToIdConverterInjection {
    default Map<ResourceLocation, Resource> listMatchingResourcesFromNamespace(ResourceManager manager, String namespace) {
        throw NytheriaCompatHelper.createMixinException(FileToIdConverterInjection.class, "listMatchingResourcesFromNamespace");
    }

}
