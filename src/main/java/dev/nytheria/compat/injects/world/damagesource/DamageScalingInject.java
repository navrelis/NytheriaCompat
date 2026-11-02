/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.damagesource;

import net.minecraft.world.damagesource.DamageScaling;
import net.neoforged.fml.common.asm.enumextension.*;
import net.neoforged.neoforge.common.damagesource.IScalingFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.damagesource.DamageScalingInjection;

@NamedEnum
@NetworkedEnum(NetworkedEnum.NetworkCheck.CLIENTBOUND)
@Mixin(DamageScaling.class)
public abstract class DamageScalingInject implements IExtensibleEnum, DamageScalingInjection {
    @Unique
    private IScalingFunction scaling = IScalingFunction.DEFAULT;

    @ReservedConstructor
    private DamageScalingInject(String name, int ordinal, String id) {}

    @CreateInitializer
    private DamageScalingInject(String name, int ordinal, String id, IScalingFunction scaling) {
        this(name, ordinal, id);
        this.scaling = scaling;
    }

    @Override
    public IScalingFunction getScalingFunction() {
        return this.scaling;
    }

    @CreateStatic
    private static ExtensionInfo getExtensionInfo() {
        return ExtensionInfo.nonExtended(DamageScaling.class);
    }
}
