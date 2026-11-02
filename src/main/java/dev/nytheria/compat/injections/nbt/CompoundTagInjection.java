/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.nbt;

import net.minecraft.nbt.CompoundTag;
import dev.nytheria.compat.mixin.CompoundTagAccessor;

import java.util.HashMap;

public interface CompoundTagInjection {
    static CompoundTag create(int expectedEntries) {
        return CompoundTagAccessor.createCompoundTag(HashMap.newHashMap(expectedEntries));
    }
}
