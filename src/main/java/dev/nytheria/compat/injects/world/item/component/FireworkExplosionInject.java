/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item.component;

import net.minecraft.world.item.component.FireworkExplosion;
import net.neoforged.fml.common.asm.enumextension.*;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;

@Mixin(FireworkExplosion.class)
public abstract class FireworkExplosionInject {
    @IndexedEnum
    @NamedEnum(1)
    @NetworkedEnum(NetworkedEnum.NetworkCheck.BIDIRECTIONAL)
    @Mixin(FireworkExplosion.Shape.class)
    public abstract static class ShapeInject implements IExtensibleEnum {
        @CreateStatic
        private static ExtensionInfo getExtensionInfo() {
            return ExtensionInfo.nonExtended(FireworkExplosion.Shape.class);
        }
    }
}
