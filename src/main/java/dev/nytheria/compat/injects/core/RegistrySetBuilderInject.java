// TRACKED HASH: b2cb1e6d77ca3eafed7a519a77bc4a5b98e52c62
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.core;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.core.RegistrySetBuilderInjection;

import java.util.List;

@Mixin(RegistrySetBuilder.class)
public class RegistrySetBuilderInject implements RegistrySetBuilderInjection {
    @Shadow @Final public List<RegistrySetBuilder.RegistryStub<?>> entries;

    @Override
    public List<? extends ResourceKey<? extends Registry<?>>> getEntryKeys() {
        return this.entries.stream().map(RegistrySetBuilder.RegistryStub::key).toList();
    }
}