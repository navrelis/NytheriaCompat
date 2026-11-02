/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.resources;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ResourceKey.class)
public abstract class ResourceKeyInject implements Comparable<ResourceKey<?>> {
    @Shadow public abstract ResourceLocation location();

    @Shadow public abstract ResourceLocation registry();

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || this.getClass() != obj.getClass())
            return false;

        return this.location().equals(((ResourceKey<?>) obj).location()) && this.registry().equals(((ResourceKey<?>) obj).registry());
    }

    @Override
    public int compareTo(@NotNull ResourceKey<?> o) {
        int ret = this.registry().compareTo(o.registry());
        if (ret == 0)
            ret = this.location().compareTo(o.location());

        return ret;
    }
}
