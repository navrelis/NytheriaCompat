/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.packs;

import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Locale;

@Mixin(AbstractPackResources.class)
public abstract class AbstractPackResourcesInject {
    @Shadow
    @Final
    private PackLocationInfo location;

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s: %s", this.getClass().getName(), this.location.id());
    }
}
