/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.entity.item;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface ItemEntityInjection {
    default int nytheriacompat$getLifespan() {
        throw NytheriaCompatHelper.createMixinException(ItemEntityInjection.class, "nytheriacompat$getLifespan");
    }

    @Nullable
    default UUID getTarget() {
        throw NytheriaCompatHelper.createMixinException(ItemEntityInjection.class, "getTarget");
    }
}
