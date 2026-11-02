/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.multiplayer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.neoforged.neoforge.entity.PartEntity;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface ClientLevelInjection {
    default Int2ObjectMap<PartEntity<?>> nytheriacompat$getPartEntitiesMap() {
        throw NytheriaCompatHelper.createMixinException(ClientLevelInjection.class, "nytheriacompat$getPartEntitiesMap");
    }
}
