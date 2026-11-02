/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.damagesource;

import net.minecraft.world.damagesource.DamageScaling;
import net.neoforged.neoforge.common.damagesource.IScalingFunction;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@FabricInjectedInterface(DamageScaling.class)
public interface DamageScalingInjection {
    default IScalingFunction getScalingFunction() {
        throw NytheriaCompatHelper.createMixinException(DamageScalingInjection.class, "getScalingFunction");
    }
}
