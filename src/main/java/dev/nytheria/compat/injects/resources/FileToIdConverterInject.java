/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.resources;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.resources.FileToIdConverterInjection;

import java.util.Map;

@Mixin(FileToIdConverter.class)
public abstract class FileToIdConverterInject implements FileToIdConverterInjection {
    @Shadow @Final private String prefix;
    @Shadow @Final private String extension;

    @Override
    public Map<ResourceLocation, Resource> listMatchingResourcesFromNamespace(ResourceManager manager, String namespace) {
        String extension = this.extension;
        return manager.listResources(this.prefix, path -> path.getNamespace().equals(namespace) && path.getPath().endsWith(extension));
    }

}
