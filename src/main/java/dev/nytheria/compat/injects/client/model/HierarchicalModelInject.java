/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.model;

import net.neoforged.neoforge.client.entity.animation.json.AnimationHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.client.model.HierarchicalModelInjection;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AnimationState;

@Mixin(HierarchicalModel.class)
public abstract class HierarchicalModelInject implements HierarchicalModelInjection {
    @Shadow protected abstract void animate(AnimationState animationState, AnimationDefinition animationDefinition, float ageInTicks);
    @Shadow protected abstract void animateWalk(AnimationDefinition animationDefinition, float limbSwing, float limbSwingAmount, float maxAnimationSpeed, float animationScaleFactor);
    @Shadow protected abstract void animate(AnimationState animationState, AnimationDefinition animationDefinition, float ageInTicks, float speed);
    @Shadow protected abstract void applyStatic(AnimationDefinition animationDefinition);

    @CreateStatic
    private static AnimationHolder getAnimation(ResourceLocation key) {
        return HierarchicalModelInjection.getAnimation(key);
    }

    @Override
    public void animate(AnimationState state, AnimationHolder animation, float ageInTicks) {
        this.animate(state, animation.get(), ageInTicks);
    }

    @Override
    public void animateWalk(AnimationHolder animation, float limbSwing, float limbSwingAmount, float maxAnimationSpeed, float animationScaleFactor) {
        this.animateWalk(animation.get(), limbSwing, limbSwingAmount, maxAnimationSpeed, animationScaleFactor);
    }

    @Override
    public void animate(AnimationState state, AnimationHolder animation, float ageInTicks, float speed) {
        this.animate(state, animation.get(), ageInTicks, speed);
    }

    @Override
    public void applyStatic(AnimationHolder animation) {
        this.applyStatic(animation.get());
    }
}
