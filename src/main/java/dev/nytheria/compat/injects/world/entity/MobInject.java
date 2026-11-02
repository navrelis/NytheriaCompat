/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.world.entity.MobInjection;

@Mixin(Mob.class)
public abstract class MobInject extends LivingEntity implements MobInjection {
    @Shadow public abstract ItemStack getBodyArmorItem();
    @Shadow public abstract boolean isBodyArmorItem(ItemStack itemStack);

    @Unique @Nullable private MobSpawnType spawnType;
    @Unique private boolean spawnCancelled = false;

    protected MobInject(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity nytheriacompat$callChangeTargetEvent(LivingEntity value) {
        LivingChangeTargetEvent event = CommonHooks.onLivingChangeTarget(this, value, LivingChangeTargetEvent.LivingTargetType.MOB_TARGET);

        if (!event.isCanceled()) {
            return event.getNewAboutToBeSetTarget();
        }

        return null;
    }

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void nytheriacompat$storeCurrentSpawnType(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        this.spawnType = spawnType;
    }

    @Override
    public void jumpInFluid(FluidType type) {
        // Nytheria Compat TODO: make this correct
        this.jumpInLiquid(FluidTags.WATER);
    }

    @Override
    public final @Nullable MobSpawnType getSpawnType() {
        return this.spawnType;
    }

    @Override
    public final boolean isSpawnCancelled() {
        return this.spawnCancelled;
    }

    @Override
    public final void setSpawnCancelled(boolean spawnCancelled) {
        if (this.isAddedToLevel()) {
            throw new UnsupportedOperationException("Late invocations of Mob#setSpawnCancelled are not permitted.");
        }

        this.spawnCancelled = spawnCancelled;
    }
}
