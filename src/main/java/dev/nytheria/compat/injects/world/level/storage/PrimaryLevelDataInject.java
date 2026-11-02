/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.storage;

import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.nytheria.compat.injections.world.level.storage.PrimaryLevelDataInjection;

@Mixin(PrimaryLevelData.class)
public abstract class PrimaryLevelDataInject implements PrimaryLevelDataInjection, ServerLevelData {
    @Unique private boolean confirmedExperimentalWarning = false;

    @Override
    public boolean hasConfirmedExperimentalWarning() {
        return this.confirmedExperimentalWarning;
    }

    @Override
    public PrimaryLevelData withConfirmedWarning(boolean confirmedWarning) {
        this.confirmedExperimentalWarning = confirmedWarning;
        return (PrimaryLevelData) (Object) this;
    }

    @Unique private float dayTimeFraction = 0f;
    @Unique private float dayTimePerTick = -1f;

    @Override
    public float getDayTimeFraction() {
        return this.dayTimeFraction;
    }

    @Override
    public float getDayTimePerTick() {
        return this.dayTimePerTick;
    }

    @Override
    public void setDayTimeFraction(float dayTimeFraction) {
        this.dayTimeFraction = dayTimeFraction;
    }

    @Override
    public void setDayTimePerTick(float dayTimePerTick) {
        this.dayTimePerTick = dayTimePerTick;
    }
}
