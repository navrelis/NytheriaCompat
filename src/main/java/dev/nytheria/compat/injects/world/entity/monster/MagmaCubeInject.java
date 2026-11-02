/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.monster;

import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;

@Mixin(MagmaCube.class)
public abstract class MagmaCubeInject extends Slime {
    public MagmaCubeInject(EntityType<? extends Slime> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void nytheriacompat$callJumpEvent(CallbackInfo ci) {
        CommonHooks.onLivingJump(this);
    }

    // Nytheria Compat: what NeoForge's MagmaCube#jumpInFluid does: the magma cube's own jump for lava, the fluid jump of any
    //  other mob for everything else. The lava jump is the lava branch of vanilla's jumpInLiquid, so that is called.
    //  This used to route both cases through jumpInLiquid with two fields that told it what "is lava" and "call super"
    //  meant for the time of the call. For a fluid that is not lava "call super" was Mob#jumpInFluid, which itself ends
    //  in jumpInLiquid: with the fields still set that ran "call super" again, without end. A magma cube jumping in a
    //  fluid of a NeoForge mod crashed the server with a StackOverflowError that way. Nothing is kept between calls now.
    @Override
    public void jumpInFluid(FluidType type) {
        if (type == NeoForgeMod.LAVA_TYPE.value()) {
            this.jumpInLiquid(FluidTags.LAVA);
        } else {
            super.jumpInFluid(type);
        }
    }
}
