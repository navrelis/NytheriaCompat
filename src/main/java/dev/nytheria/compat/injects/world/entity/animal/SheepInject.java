/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.animal;

import net.minecraft.world.entity.animal.Sheep;
import net.neoforged.neoforge.common.IShearable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Sheep.class)
public abstract class SheepInject implements IShearable {
    // Nytheria Compat: Handled by Porting Lib
}
