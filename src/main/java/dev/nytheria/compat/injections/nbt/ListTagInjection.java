/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.nbt;

import net.minecraft.nbt.ListTag;
import dev.nytheria.compat.mixin.ListTagAccessor;

import java.util.ArrayList;

public interface ListTagInjection {
    static ListTag create(int initialCapacity) {
        return ListTagAccessor.createListTag(new ArrayList<>(initialCapacity), (byte) 0);
    }
}
