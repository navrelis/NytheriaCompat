/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.workarounds.mapped_registry_aliases;

import net.fabricmc.fabric.api.event.registry.FabricRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.BaseMappedRegistry;
import org.spongepowered.asm.mixin.*;
import dev.nytheria.compat.loader.NytheriaCompatLoader;
import dev.nytheria.compat.workarounds.MappedRegistryWorkaround;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@SuppressWarnings("NonExtendableApiUsage")
@Mixin(value = MappedRegistry.class, priority = 1050)
@Implements(@Interface(iface = MappedRegistryWorkaround.class, prefix = "nytheriacompat$i$"))
public abstract class MappedRegistryMixin implements FabricRegistry {

    @Unique
    private static final MethodHandle nytheriacompat$super$addAlias;

    static {
        try {
            // https://stackoverflow.com/a/15674467
            //noinspection JavaLangInvokeHandleSignature
            nytheriacompat$super$addAlias = MethodHandles.lookup().findSpecial(
                BaseMappedRegistry.class, "addAlias",
                MethodType.methodType(Void.TYPE, ResourceLocation.class, ResourceLocation.class),
                MappedRegistry.class
            );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Intrinsic(displace = true)
    public void nytheriacompat$i$addAlias(ResourceLocation old, ResourceLocation newId) {
        if (NytheriaCompatLoader.Companion.getInstance().hasMod(old.getNamespace())) {
            try {
                //noinspection JavaLangInvokeHandleSignature
                nytheriacompat$super$addAlias.invoke(this, old, newId);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            addAlias(old, newId);
        }
    }

}
