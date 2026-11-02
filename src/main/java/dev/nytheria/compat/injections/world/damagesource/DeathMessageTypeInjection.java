/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.damagesource;

import net.minecraft.world.damagesource.DeathMessageType;
import net.neoforged.neoforge.common.damagesource.IDeathMessageProvider;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@FabricInjectedInterface(DeathMessageType.class)
public interface DeathMessageTypeInjection {
    default IDeathMessageProvider getMessageFunction() {
        throw NytheriaCompatHelper.createMixinException(DeathMessageTypeInjection.class, "getMessageFunction");
    }
}
