/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.packs.repository;

import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.server.packs.repository.BuiltInPackSourceInjection;

import java.util.function.Function;

@Mixin(BuiltInPackSource.class)
public abstract class BuiltInPackSourceInject implements BuiltInPackSourceInjection {
    @CreateStatic
    private static Pack.ResourcesSupplier fromName(final Function<PackLocationInfo, PackResources> onName) {
        return BuiltInPackSourceInjection.fromName(onName);
    }
}
