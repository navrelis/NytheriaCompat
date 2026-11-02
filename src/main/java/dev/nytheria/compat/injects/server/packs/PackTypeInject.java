/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.packs;

import net.minecraft.server.packs.PackType;
import net.minecraft.util.StringRepresentable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PackType.class)
public abstract class PackTypeInject implements StringRepresentable {
    @Override
    public String getSerializedName() {
        return ((Enum) (Object) this).name().toLowerCase();
    }
}
