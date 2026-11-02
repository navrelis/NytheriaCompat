/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.storage;

import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.world.level.storage.LevelStorageAccessInjection;


@Mixin(LevelStorageSource.class)
public abstract class LevelStorageSourceInject {
    @Mixin(LevelStorageSource.LevelStorageAccess.class)
    public abstract static class LevelStorageAccessInject implements LevelStorageAccessInjection {
        @Shadow @Final private LevelStorageSource field_23766;
        @Shadow protected abstract void checkLock();
        @Shadow @Final private LevelStorageSource.LevelDirectory levelDirectory;

    }
}
