/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.workarounds;

import net.neoforged.neoforge.fluids.FluidType;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface FluidWorkaround {
    default FluidType getFluidType() {
        throw NytheriaCompatHelper.createMixinException(FluidWorkaround.class, "getFluidType");
    }
}
