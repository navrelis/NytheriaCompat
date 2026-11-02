/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.animal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.IShearable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SnowGolem.class)
public abstract class SnowGolemInject extends AbstractGolem implements IShearable {
    protected SnowGolemInject(EntityType<? extends AbstractGolem> entityType, Level level) {
        super(entityType, level);
    }

    // Nytheria Compat: Shearing handled by Porting Lib
}
