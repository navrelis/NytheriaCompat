// TRACKED HASH: 171de07c400ad7a7aa28f0ddfa4684a0fda24e32
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity;

import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.injections.world.entity.LightningBoltInjection;

@Mixin(LightningBolt.class)
public abstract class LightningBoltInject implements LightningBoltInjection {
    @Unique private float damage = 5.0f;

    @Override
    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public float getDamage() {
        return this.damage;
    }

}