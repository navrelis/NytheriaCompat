/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.fluids.sound.SoundAction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.pathfinder.PathType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(FluidType.class)
public interface FluidTypeAccessor {
    @Accessor("descriptionId")
    String nytheriacompat$getDescriptionId();

    @Accessor("descriptionId")
    void nytheriacompat$setDescriptionId(String id);

    @Accessor
    double getMotionScale();

    @Accessor
    boolean isCanPushEntity();

    @Accessor
    boolean isCanSwim();

    @Accessor
    boolean isCanDrown();

    @Accessor
    float getFallDistanceModifier();

    @Accessor
    boolean isCanExtinguish();

    @Accessor
    boolean isCanConvertToSource();

    @Accessor
    boolean isSupportsBoating();

    @Accessor
    PathType getPathType();

    @Accessor
    PathType getAdjacentPathType();

    @Accessor
    boolean isCanHydrate();

    @Accessor
    int getLightLevel();

    @Accessor
    int getDensity();

    @Accessor
    int getTemperature();

    @Accessor
    int getViscosity();

    @Accessor
    Rarity getRarity();

    @Accessor
    Map<SoundAction, SoundEvent> getSounds();
}
