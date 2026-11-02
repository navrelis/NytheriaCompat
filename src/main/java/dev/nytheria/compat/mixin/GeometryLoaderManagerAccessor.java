/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin;

import com.google.common.collect.ImmutableMap;
import io.github.fabricators_of_create.porting_lib.models.geometry.GeometryLoaderManager;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GeometryLoaderManager.class, remap = false)
public interface GeometryLoaderManagerAccessor {
    @Accessor(value = "LOADERS", remap = false)
    static ImmutableMap<ResourceLocation, IGeometryLoader<?>> getLoaders() {
        throw new UnsupportedOperationException();
    }

    @Accessor(value = "LOADER_LIST", remap = false)
    static String getLoaderList() {
        throw new UnsupportedOperationException();
    }

    @Accessor(value = "LOADERS", remap = false)
    static void setLoaders(ImmutableMap<ResourceLocation, IGeometryLoader<?>> list) {
        throw new UnsupportedOperationException();
    }

    @Accessor(value = "LOADER_LIST", remap = false)
    static void setLoaderList(String list) {
        throw new UnsupportedOperationException();
    }
}
