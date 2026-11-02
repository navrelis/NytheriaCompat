/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.damagesource;

import net.minecraft.world.damagesource.DeathMessageType;
import net.neoforged.fml.common.asm.enumextension.*;
import net.neoforged.neoforge.common.damagesource.IDeathMessageProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.damagesource.DeathMessageTypeInjection;

@NamedEnum
@NetworkedEnum(NetworkedEnum.NetworkCheck.CLIENTBOUND)
@Mixin(DeathMessageType.class)
public abstract class DeathMessageTypeInject implements IExtensibleEnum, DeathMessageTypeInjection {
    @Unique
    private IDeathMessageProvider msgFunction = IDeathMessageProvider.DEFAULT;

    @ReservedConstructor
    private DeathMessageTypeInject(String name, int ordinal, String id) {}

    @CreateInitializer
    private DeathMessageTypeInject(String name, int ordinal, String id, IDeathMessageProvider msgFunction) {
        this(name, ordinal, id);
        this.msgFunction = msgFunction;
    }

    @Override
    public IDeathMessageProvider getMessageFunction() {
        return this.msgFunction;
    }

    @CreateStatic
    private static ExtensionInfo getExtensionInfo() {
        return ExtensionInfo.nonExtended(DeathMessageType.class);
    }
}
