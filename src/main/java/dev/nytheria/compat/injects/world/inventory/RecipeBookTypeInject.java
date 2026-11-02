// TRACKED HASH: d27191d511ec2b22a36acfc37e2404e5b0edc805
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.inventory;

import net.minecraft.world.inventory.RecipeBookType;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;

@NetworkedEnum(NetworkedEnum.NetworkCheck.CLIENTBOUND)
@Mixin(RecipeBookType.class)
public class RecipeBookTypeInject implements IExtensibleEnum {
    @CreateStatic
    private static ExtensionInfo getExtensionInfo() {
        return ExtensionInfo.nonExtended(RecipeBookType.class);
    }
}
