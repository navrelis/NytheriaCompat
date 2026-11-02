/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.animal;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Fox.class)
public abstract class FoxInject extends Animal {
    protected FoxInject(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    // TODO: How do we go about the dropEquipment patch?

    @Mixin(targets = "net.minecraft.world.entity.animal.Fox$FoxBreedGoal")
    public abstract static class FoxBreedGoalInject extends BreedGoal {
        public FoxBreedGoalInject(Animal animal, double speedModifier) {
            super(animal, speedModifier);
        }
    }

    @Mixin(Fox.FoxEatBerriesGoal.class)
    public abstract static class FoxEatBerriesGoalInject extends MoveToBlockGoal {

        public FoxEatBerriesGoalInject(PathfinderMob mob, double speedModifier, int searchRange) {
            super(mob, speedModifier, searchRange);
        }

    }

    @Mixin(targets = "net.minecraft.world.entity.animal.Fox$FoxFloatGoal")
    public abstract static class FoxFloatGoalInject extends FloatGoal {
        @Shadow @Final private Fox field_17976;

        public FoxFloatGoalInject(Mob mob) {
            super(mob);
        }

        @ModifyExpressionValue(method = "canUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Fox;isInLava()Z"))
        private boolean nytheriacompat$checkIsInFluidType(boolean original) {
            var fox = field_17976;
            return original || (fox.isInFluidType() && fox.isInFluidType((fluidType, height) -> fox.canSwimInFluidType(fluidType) && height > 0.25));
        }
    }
}
