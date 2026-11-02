/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.world.level.chunk.storage.SectionStorageInjection;

import java.util.Optional;

@Mixin(SectionStorage.class)
public abstract class SectionStorageInject<R> implements SectionStorageInjection {
    @Shadow @Final private Long2ObjectMap<Optional<R>> storage;

    @Override
    public void remove(long sectionPosAsLong) {
        this.storage.remove(sectionPosAsLong);
    }
}
