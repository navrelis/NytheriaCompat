/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.entity;

import net.minecraft.world.level.entity.EntityAccess;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface PersistentEntitySectionManagerInjection<T extends EntityAccess> {
    default void nytheriacompat$markWithoutEvent() {
        throw NytheriaCompatHelper.createMixinException(PersistentEntitySectionManagerInjection.class, "nytheriacompat$markWithoutEvent");
    }

}
