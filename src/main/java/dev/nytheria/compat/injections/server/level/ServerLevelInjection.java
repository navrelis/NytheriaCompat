/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.server.level;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import net.neoforged.neoforge.entity.PartEntity;
import dev.nytheria.compat.injections.world.level.LevelInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface ServerLevelInjection extends LevelInjection {
    default Int2ObjectMap<PartEntity<?>> nytheriacompat$getEntityParts() {
        throw NytheriaCompatHelper.createMixinException(ServerLevelInjection.class, "nytheriacompat$getEntityParts");
    }

    default void registerCapabilityListener(BlockPos pos, ICapabilityInvalidationListener listener) {
        throw NytheriaCompatHelper.createMixinException(ServerLevelInjection.class, "registerCapabilityListener");
    }

    default void cleanCapabilityListenerReferences() {
        throw NytheriaCompatHelper.createMixinException(ServerLevelInjection.class, "cleanCapabilityListenerReferences");
    }

    @Override
    default void setDayTimeFraction(float dayTimeFraction) {
        throw NytheriaCompatHelper.createMixinException(ServerLevelInjection.class, "setDayTimeFraction");
    }

    @Override
    default float getDayTimeFraction() {
        throw NytheriaCompatHelper.createMixinException(ServerLevelInjection.class, "getDayTimeFraction");
    }

    @Override
    default float getDayTimePerTick() {
        throw NytheriaCompatHelper.createMixinException(ServerLevelInjection.class, "getDayTimePerTick");
    }
}
