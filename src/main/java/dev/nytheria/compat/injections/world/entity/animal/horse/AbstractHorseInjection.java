/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.entity.animal.horse;

import net.minecraft.world.Container;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface AbstractHorseInjection {
    default Container getInventory() {
        throw NytheriaCompatHelper.createMixinException(AbstractHorseInjection.class, "getInventory");
    }
}
