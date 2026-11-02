// TRACKED HASH: 0103ffc8bca3b91dd898021eb13bdca66921d3eb
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import dev.nytheria.compat.hook.EventGate;
import dev.nytheria.compat.hook.Need;
import dev.nytheria.compat.hook.NeoFluids;
import dev.nytheria.compat.hook.Scope;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.extensions.ILivingEntityExtension;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.world.entity.LivingEntityInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

@Mixin(LivingEntity.class)
public abstract class LivingEntityInject extends Entity implements ILivingEntityExtension, LivingEntityInjection {
    public LivingEntityInject(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow public abstract ItemStack getItemInHand(InteractionHand hand);
    @Shadow @Final private Map<MobEffect, MobEffectInstance> activeEffects;
    @Shadow protected abstract void onEffectRemoved(MobEffectInstance effectInstance);
    @Shadow private boolean effectsDirty;
    @Shadow protected ItemStack useItem;
    @Shadow protected int useItemRemaining;
    @Shadow public abstract int getUseItemRemainingTicks();
    @Shadow protected float lastHurt;
    @Shadow private Optional<BlockPos> lastClimbablePos;
    @Shadow public abstract ItemStack getMainHandItem();
    @Shadow public abstract boolean isUsingItem();
    @Shadow protected abstract void completeUsingItem();
    @Shadow public abstract double getAttributeValue(Holder<Attribute> attribute);
    @Shadow protected int fallFlyTicks;
    @Shadow protected abstract int increaseAirSupply(int currentAir);
    @Shadow protected abstract int decreaseAirSupply(int currentAir);
    @Shadow protected boolean dead;

    @Nullable
    protected Stack<DamageContainer> damageContainers = new Stack<>();

    @Override
    public Stack<DamageContainer> nytheriacompat$getDamageContainers() {
        return this.damageContainers;
    }

    @ModifyReturnValue(method = "createLivingAttributes", at = @At("RETURN"))
    private static AttributeSupplier.Builder nytheriacompat$addNeoAttributes(AttributeSupplier.Builder original) {
        return original
            .add(NeoForgeMod.SWIM_SPEED);
    }

    // Nytheria Compat: we're reimplementing onLivingBreathe from scratch here for the sake of mod compatibility
    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z", ordinal = 0))
    private void nytheriacompat$tryHandleLivingBreathe(CallbackInfo ci) {
        if (!EventGate.has(LivingBreatheEvent.class)) return;

        LivingEntity entity = (LivingEntity) (Object) this;
        int airSupply = this.getAirSupply();
        int consumeAirAmount = airSupply - this.decreaseAirSupply(airSupply);
        int refillAirAmount = this.increaseAirSupply(airSupply) - airSupply;

        // Check things that vanilla considers to be air - these will cause the air supply to be increased.
        boolean isAir = entity.getEyeInFluidType().isAir() || entity.level().getBlockState(BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ())).is(Blocks.BUBBLE_COLUMN);
        boolean canBreathe = isAir;
        // The following effects cause the entity to not drown, but do not cause the air supply to be increased.
        if (!isAir && (MobEffectUtil.hasWaterBreathing(entity) || !entity.canDrownInFluidType(entity.getEyeInFluidType()) || (entity instanceof Player player && player.getAbilities().invulnerable))) {
            canBreathe = true;
            refillAirAmount = 0;
        }

        LivingBreatheEvent breatheEvent = new LivingBreatheEvent(entity, canBreathe, consumeAirAmount, refillAirAmount);
        NeoForge.EVENT_BUS.post(breatheEvent);

        if (breatheEvent.nytheriacompat$canBreatheModified) {
            if (breatheEvent.canBreathe()) {
                entity.setAirSupply(Math.min(entity.getAirSupply() + breatheEvent.getRefillAirAmount(), entity.getMaxAirSupply()));
            } else {
                entity.setAirSupply(entity.getAirSupply() - breatheEvent.getConsumeAirAmount());
            }
        }
    }

    @WrapOperation(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean nytheriacompat$checkIsDrownableFluid(LivingEntity instance, TagKey tagKey, Operation<Boolean> original) {
        return original.call(instance, tagKey) || (NeoFluids.isNeoType(instance.getEyeInFluidType()) && instance.canDrownInFluidType(instance.getEyeInFluidType()));
    }

    @Definition(id = "getAirSupply", method = "Lnet/minecraft/world/entity/LivingEntity;getAirSupply()I")
    @Expression("this.getAirSupply() == -20")
    @Inject(method = "baseTick", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void nytheriacompat$initDrownEvent(CallbackInfo ci, @Share("drownEvent") LocalRef<LivingDrownEvent> eventRef) {
        if (EventGate.has(LivingDrownEvent.class)) eventRef.set(new LivingDrownEvent((LivingEntity) (Object) this));
    }

    @Definition(id = "getAirSupply", method = "Lnet/minecraft/world/entity/LivingEntity;getAirSupply()I")
    @Expression("this.getAirSupply() == -20")
    @ModifyExpressionValue(method = "baseTick", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$callDrownEvent(boolean original, @Share("drownEvent") LocalRef<LivingDrownEvent> eventRef) {
        if (!EventGate.has(LivingDrownEvent.class)) return original;

        var drownEvent = eventRef.get();
        if (drownEvent == null) {
            drownEvent = new LivingDrownEvent((LivingEntity) (Object) this);
            eventRef.set(drownEvent);
        }

        return !NeoForge.EVENT_BUS.post(drownEvent).isCanceled() && drownEvent.isDrowning();
    }

    @WrapOperation(method = "baseTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;isInPowderSnow:Z"))
    private boolean nytheriacompat$checkIfCanExtinguish(LivingEntity instance, Operation<Boolean> original) {
        return original.call(instance) || (instance.isInFluidType() && instance.isInFluidType((fluidType, height) -> instance.canFluidExtinguish(fluidType)));
    }

    @WrapOperation(method = "updateSynchronizedMobEffectParticles", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    private <T extends MobEffectInstance> Stream<T> nytheriacompat$callAndUseEffectParticleModifyEvent(Stream<T> instance, Predicate<? super T> predicate, Operation<Stream<T>> original, @Share("events") LocalRef<Map<MobEffectInstance, EffectParticleModificationEvent>> eventsRef) {
        if (!EventGate.has(EffectParticleModificationEvent.class)) return original.call(instance, predicate);

        eventsRef.set(new HashMap<>());
        LivingEntity self = (LivingEntity) (Object) this;

        return original.call(instance.peek(effect -> { if (EventGate.has(EffectParticleModificationEvent.class)) eventsRef.get().put(effect, NeoForge.EVENT_BUS.post(new EffectParticleModificationEvent(self, effect))); }),
            (Predicate<T>) effect -> {
                var event = eventsRef.get().get(effect);

                if (event.nytheriacompat$wasVisibilityModified()) {
                    return event.isVisible();
                }

                return predicate.test(effect);
            });
    }

    @WrapWithCondition(method = "removeAllEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onEffectRemoved(Lnet/minecraft/world/effect/MobEffectInstance;)V"))
    private boolean nytheriacompat$callRemoveEffectEvent(LivingEntity instance, MobEffectInstance effectInstance, @Share("shouldRemove") LocalBooleanRef shouldCancel) {
        // Nytheria Compat: the event decides per effect, a cancelled one must not keep the next ones in the map.
        shouldCancel.set(EventHooks.onEffectRemoved(instance, effectInstance, null));
        return !shouldCancel.get();
    }

    @WrapWithCondition(method = "removeAllEffects", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V"))
    private boolean nytheriacompat$checkIfCancelledAlready(Iterator<?> instance, @Share("shouldRemove") LocalBooleanRef shouldCancel) {
        return !shouldCancel.get();
    }

    @Inject(method = "removeEffect", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$checkRemoveEffect(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (EventHooks.onEffectRemoved((LivingEntity) (Object) this, effect, null))
            cir.setReturnValue(false);
    }

    // Nytheria Compat: vanilla lets heal(<= 0) through, mods use it, so only the event's result can stop a heal.
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$callHealEvent(float value, CallbackInfo ci, @Local(argsOnly = true) LocalFloatRef healAmount) {
        healAmount.set(EventHooks.onLivingHeal((LivingEntity) (Object) this, value));
        if (healAmount.get() != value && healAmount.get() <= 0)
            ci.cancel();
    }

    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"), cancellable = true)
    private void nytheriacompat$pushNewDamageContainer(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        damageContainers.push(new DamageContainer(source, amount));

        if (CommonHooks.onEntityIncomingDamage((LivingEntity) (Object) this, damageContainers.peek()))
            cir.setReturnValue(false);
    }

    // Nytheria Compat: the only pop: every return after the push counts, and other mods' cancellable injects add returns of their own.
    //  A container left behind would be read by an outer hurt on this entity and hold its DamageSource for the entity's lifetime.
    @WrapMethod(method = "hurt")
    private boolean nytheriacompat$dropCancelledContainers(DamageSource source, float amount, Operation<Boolean> original) {
        int depth = this.damageContainers.size();
        boolean result = original.call(source, amount);
        this.damageContainers.setSize(depth);
        return result;
    }

    @ModifyVariable(method = "hurt", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noActionTime:I", shift = At.Shift.AFTER), argsOnly = true)
    private float nytheriacompat$modifyDamage(float value) {
        DamageContainer container = this.damageContainers.peek();
        if (value != container.getOriginalDamage())
            return value;

        return container.getNewDamage();
    }

    @ModifyExpressionValue(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDamageSourceBlocked(Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean nytheriacompat$checkIsDamageBlocked(boolean original, @Share("shieldEvent") LocalRef<LivingShieldBlockEvent> shieldEvent) {
        shieldEvent.set(CommonHooks.onDamageBlock((LivingEntity) (Object) this, damageContainers.peek(), original));

        return shieldEvent.get().getBlocked();
    }

    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"))
    private void nytheriacompat$checkShouldHurtCurrentShield(LivingEntity instance, float damageAmount, Operation<Void> original, @Share("shieldEvent") LocalRef<LivingShieldBlockEvent> shieldEvent) {
        // Nytheria Compat: getOriginalBlockedDamage reads the container, so it has to be compared before setBlockedDamage lowers it.
        boolean moddedDamage = damageAmount != shieldEvent.get().getOriginalBlockedDamage();
        damageContainers.peek().setBlockedDamage(shieldEvent.get());

        if (moddedDamage) {
            original.call(instance, damageAmount); // Ensure modded damage goes through instead of ours.
        } else if (shieldEvent.get().shieldDamage() > 0) {
            original.call(instance, shieldEvent.get().shieldDamage());
        }
    }

    // Nytheria Compat: at the stat, not at the store of the local: Porting Lib entity's modifyBlockedDamage writes the local after the store from its own ShieldBlockEvent, which never sees this event.
    @ModifyArg(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;awardStat(Lnet/minecraft/resources/ResourceLocation;I)V"))
    private int nytheriacompat$modifyBlockedDamageStat(int original) {
        return Math.min(original, Math.round(damageContainers.peek().getBlockedDamage() * 10.0F));
    }

    @Definition(id = "amount", local = @Local(type = float.class, ordinal = 0, argsOnly = true))
    @Expression("amount = @(0.0)")
    @ModifyExpressionValue(method = "hurt", at = @At("MIXINEXTRAS:EXPRESSION"))
    private float nytheriacompat$modifyTotalDamage(float original, @Share("shieldEvent") LocalRef<LivingShieldBlockEvent> shieldEvent) {
        if (original != 0.0) {
            return original;
        }

        return shieldEvent.get().getDamageContainer().getNewDamage();
    }

    @Definition(id = "bl", local = @Local(type = boolean.class, ordinal = 0))
    @Expression("bl = @(true)")
    @ModifyExpressionValue(method = "hurt", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$checkIsDamageAmountFullyBlocked(boolean original, @Local(argsOnly = true) float damage) {
        return original && damage <= 0;
    }

    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;setSpeed(F)V"))
    private void nytheriacompat$updateContainerWithVanillaChanges(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        damageContainers.peek().setNewDamage(amount); //update container with vanilla changes
    }

    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", ordinal = 0))
    private void nytheriacompat$setContainerReductionByInvulnerability(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        damageContainers.peek().setReduction(DamageContainer.Reduction.INVULNERABILITY, this.lastHurt);
    }

    @Definition(id = "invulnerableTime", field = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I")
    @Expression("this.invulnerableTime = @(20)")
    @ModifyExpressionValue(method = "hurt", at = @At("MIXINEXTRAS:EXPRESSION"))
    private int nytheriacompat$modifyPostAttackInvulnerabilityTicks(int original) {
        DamageContainer container = damageContainers.peek();

        if (original != 20) {
            return original;
        }

        return container.getPostAttackInvulnerabilityTicks();
    }

    // TODO: implement TamableAnimal instanceof check

    @WrapOperation(method = "checkTotemDeathProtection", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;removeAllEffects()Z"))
    private boolean nytheriacompat$removeTotemEffects(LivingEntity instance, Operation<Boolean> original) {
        var effects = this.activeEffects.values();
        var shouldUseCures = false;

        // Nytheria Compat: Mod compatibility :D
        for (MobEffectInstance effect : effects) {
            if (!effect.neoforge$getCures().contains(EffectCures.PROTECTED_BY_TOTEM)) {
                shouldUseCures = true;
                break;
            }
        }

        if (shouldUseCures)
            return instance.removeEffectsCuredBy(EffectCures.PROTECTED_BY_TOTEM);
        else
            return original.call(instance);
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$checkLivingDeath(DamageSource damageSource, CallbackInfo ci) {
        if (this.nytheriacompat$postVanillaLivingDeath(damageSource))
            ci.cancel();
    }

    // Nytheria Compat: set while LivingDeathEvent is posted from inside a vanilla die() (LivingEntity, Player, ServerPlayer),
    //  where Architectury's own Fabric mixin already fires LIVING_DEATH. A depth counter, so nested posts unwind correctly.
    @Unique private int nytheriacompat$vanillaLivingDeathDepth;

    @Override
    public boolean nytheriacompat$postVanillaLivingDeath(DamageSource source) {
        this.nytheriacompat$vanillaLivingDeathDepth++;
        try {
            return CommonHooks.onLivingDeath((LivingEntity) (Object) this, source);
        } finally {
            this.nytheriacompat$vanillaLivingDeathDepth--;
        }
    }

    @Override
    public boolean nytheriacompat$isPostingVanillaLivingDeath() {
        return this.nytheriacompat$vanillaLivingDeathDepth > 0;
    }

    // Nytheria Compat: NeoForge mods may override die() without calling super and post LivingDeathEvent themselves
    //  (e.g. L_Ender's Cataclysm's Animation_Monsters, which reimplements the rest of die() by hand).
    //  Fabric API fires AFTER_KILLED_OTHER_ENTITY and AFTER_DEATH from inside LivingEntity.die, so those deaths
    //  would never reach Fabric mods. Both fields are filled by listeners registered in NytheriaCompat.registerFabricEvents.
    @Unique @Nullable private LivingDeathEvent nytheriacompat$pendingDeathEvent;
    @Unique private boolean nytheriacompat$fabricDeathReported;

    @Override
    public void nytheriacompat$setPendingDeathEvent(LivingDeathEvent event) {
        this.nytheriacompat$pendingDeathEvent = event;
    }

    @Override
    public void nytheriacompat$markFabricDeathReported() {
        this.nytheriacompat$fabricDeathReported = true;
    }

    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;die(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void nytheriacompat$reportModdedDeathToFabric(LivingEntity instance, DamageSource source, Operation<Void> original) {
        var wasAlive = !this.isRemoved() && !this.dead;
        var previousEvent = this.nytheriacompat$pendingDeathEvent;
        var previousReported = this.nytheriacompat$fabricDeathReported;
        this.nytheriacompat$pendingDeathEvent = null;
        this.nytheriacompat$fabricDeathReported = false;

        try {
            original.call(instance, source);

            // Only report deaths that went through an uncancelled LivingDeathEvent and that Fabric hasn't seen yet,
            // so vanilla, Fabric and super-calling NeoForge entities (and ServerPlayer) still fire exactly once.
            // Nytheria Compat: the death must also have actually happened, a later die() HEAD hook (e.g. Architectury's LIVING_DEATH
            //  on Fabric, which runs after ours) can still cancel die() after our LivingDeathEvent went through uncancelled.
            var event = this.nytheriacompat$pendingDeathEvent;
            var died = this.dead || this.isRemoved();
            if (wasAlive && died && event != null && !event.isCanceled() && !this.nytheriacompat$fabricDeathReported && this.level() instanceof ServerLevel serverLevel) {
                var killer = source.getEntity();

                if (killer != null)
                    ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.invoker().afterKilledOtherEntity(serverLevel, killer, instance);

                ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath(instance, source);
            }
        } finally {
            this.nytheriacompat$pendingDeathEvent = previousEvent;
            this.nytheriacompat$fabricDeathReported = previousReported;
        }
    }

    // Looting Level and Capture Drops events handled by Porting Lib

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$modifyKnockback(CallbackInfo ci, @Local(argsOnly = true, ordinal = 0) LocalDoubleRef strength, @Local(argsOnly = true, ordinal = 1) LocalDoubleRef x, @Local(argsOnly = true, ordinal = 2) LocalDoubleRef z) {
        // Nytheria Compat: the event is for NeoForge mods' mobs only (Cataclysm), any other knockback is vanilla's
        if (!EventGate.has(LivingKnockBackEvent.class) || !Need.livingKnockBack((LivingEntity) (Object) this)) return;

        var event = CommonHooks.onLivingKnockBack((LivingEntity) (Object) this, (float) strength.get(), x.get(), z.get());

        if (event.isCanceled())
            ci.cancel();

        strength.set(event.getStrength());
        x.set(event.getRatioX());
        z.set(event.getRatioZ());
    }

    @Inject(method = "onClimbable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z", ordinal = 0), cancellable = true)
    private void nytheriacompat$tryUseNeoLadderPos(CallbackInfoReturnable<Boolean> cir, @Local BlockState state, @Local BlockPos pos) {
        if (!Scope.isNeoObject(state.getBlock()))
            return;

        var ladderPos = CommonHooks.isLivingOnLadder(state, this.level(), pos, (LivingEntity) (Object) this);

        if (ladderPos.isPresent()) {
            this.lastClimbablePos = ladderPos;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$checkIfCancelledFallDamage(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true, ordinal = 0) LocalFloatRef fallDistance, @Local(argsOnly = true, ordinal = 1) LocalFloatRef multiplier) {
        var values = CommonHooks.onLivingFall((LivingEntity) (Object) this, fallDistance.get(), multiplier.get());

        if (values == null) {
            cir.setReturnValue(false);
            return;
        }

        fallDistance.set(values[0]);
        multiplier.set(values[1]);
    }

    // TODO: handle custom Forge sound type

    @Definition(id = "ServerPlayer", type = ServerPlayer.class)
    @Expression("this instanceof ServerPlayer")
    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void nytheriacompat$reduceEffectDamageInContainer(DamageSource damageSource, float damageAmount, CallbackInfoReturnable<Float> cir, @Local(ordinal = 3) float reduced) {
        if (this.damageContainers.isEmpty()) return; // Nytheria Compat: mods also call this outside hurt.
        this.damageContainers.peek().setReduction(DamageContainer.Reduction.MOB_EFFECTS, reduced);
    }

    @ModifyExpressionValue(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"))
    private float nytheriacompat$reduceEnchantDamageInContainer(float original) {
        if (this.damageContainers.isEmpty()) return original;
        this.damageContainers.peek().setReduction(DamageContainer.Reduction.ENCHANTMENTS, this.damageContainers.peek().getNewDamage() - original);
        return original;
    }

    // Nytheria Compat: I want you all to know that I really, really, *really* don't like the damage containers system.
    //       It's so terribly convoluted, and it doesn't even make the code particularly extendable.
    //       And it honestly makes the mod compatibility so much more annoying, because dear god I'm so
    //       bloody worried about mixins that are actually using this that we're basically forced to overwrite.

    // Nytheria Compat: the amount in actuallyHurt stays the one that counts, Fabric mods change it on its way (Deeper Darker, Immersive Armors,
    //  Puzzles Lib, Porting Lib, Eternal Starlight, Aether), so the container follows it instead of replacing it. A NeoForge reduction
    //  function would be overruled by this; none of the four mods adds one.
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float nytheriacompat$tryReduceWithArmorAbsorb(LivingEntity instance, DamageSource damageSource, float damageAmount, Operation<Float> original) {
        DamageContainer container = this.damageContainers.peek();

        var reduced = original.call(instance, damageSource, damageAmount);
        container.setReduction(DamageContainer.Reduction.ARMOR, container.getNewDamage() - reduced);

        return reduced;
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private void nytheriacompat$callLivingPreDamageEvent(DamageSource damageSource, float damageAmount, CallbackInfo ci, @Share("damage") LocalFloatRef damageRef) {
        DamageContainer container = this.damageContainers.peek();
        container.setNewDamage(damageAmount);
        damageRef.set(CommonHooks.onLivingDamagePre((LivingEntity) (Object) this, container));
    }

    // Nytheria Compat: the event is posted before absorption like in NeoForge. Without a listener the result is vanilla's.
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float nytheriacompat$doAbsorptionModification(float a, float b, Operation<Float> original, @Share("damage") LocalFloatRef damageRef) {
        DamageContainer container = this.damageContainers.peek();
        container.setReduction(DamageContainer.Reduction.ABSORPTION, Math.min(damageRef.get(), ((LivingEntity) (Object) this).getAbsorptionAmount()));
        float vanilla = original.call(a, b);
        return EventGate.has(LivingDamageEvent.Pre.class) ? container.getNewDamage() : vanilla;
    }

    @Definition(id = "damageAmount", local = @Local(type = float.class, ordinal = 0, argsOnly = true))
    @Definition(id = "f", local = @Local(type = float.class, ordinal = 1))
    @Expression("f - damageAmount")
    @ModifyExpressionValue(method = "actuallyHurt", at = @At("MIXINEXTRAS:EXPRESSION"))
    private float nytheriacompat$useAbsorbedDamage(float original, @Share("damage") LocalFloatRef damageRef) {
        if (!EventGate.has(LivingDamageEvent.Pre.class)) return original;
        return Math.min(damageRef.get(), this.damageContainers.peek().getReduction(DamageContainer.Reduction.ABSORPTION));
    }

    @ModifyArg(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V"))
    private float nytheriacompat$clampAbsorptionAmount(float absorptionAmount) {
        return Math.max(absorptionAmount, 0);
    }

    // Nytheria Compat: Porting Lib cancels actuallyHurt for damage <= 0 (a fully blocked hit), so a TAIL inject would not run there.
    @WrapMethod(method = "actuallyHurt")
    private void nytheriacompat$callLivingPostDamageEvent(DamageSource damageSource, float damageAmount, Operation<Void> original) {
        // Nytheria Compat: Let's Do Furniture's PellsEntity.kill calls actuallyHurt without hurt, so there is no container of hurt to use.
        boolean outsideHurt = this.damageContainers.isEmpty();
        if (outsideHurt) this.damageContainers.push(new DamageContainer(damageSource, damageAmount));
        this.damageContainers.peek().setNewDamage(damageAmount);

        original.call(damageSource, damageAmount);

        if (!this.isInvulnerableTo(damageSource)) {
            CommonHooks.onLivingDamagePost((LivingEntity) (Object) this, this.damageContainers.peek());
        }
        if (outsideHurt) this.damageContainers.pop();
    }

    @Inject(method = "swapHandItems", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$callSwapHandItemsEvent(CallbackInfo ci) {
        if (CommonHooks.onLivingSwapHandItems((LivingEntity) (Object) this).isCanceled())
            ci.cancel();
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void nytheriacompat$callJumpEvent(CallbackInfo ci) {
        CommonHooks.onLivingJump((LivingEntity) (Object) this);
    }

    @Definition(id = "add", method = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;")
    @Expression("?.add(?, @(?), ?)")
    @ModifyExpressionValue(method = "jumpInLiquid", at = @At("MIXINEXTRAS:EXPRESSION"))
    private double nytheriacompat$adjustWithSwimSpeed(double original) {
        return original * this.getAttributeValue(NeoForgeMod.SWIM_SPEED);
    }

    @WrapOperation(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z", ordinal = 0))
    private boolean nytheriacompat$checkIsInNeoFluidType(LivingEntity instance, Operation<Boolean> original, @Local FluidState fluidState) {
        return original.call(instance) || (instance.isInFluidType(fluidState) && fluidState.neo$getFluidType() != NeoForgeMod.LAVA_TYPE.value());
    }

    @ModifyVariable(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V", ordinal = 0), ordinal = 1)
    private float nytheriacompat$adjustMovementToSwimSpeed(float original) {
        return original * (float) this.getAttributeValue(NeoForgeMod.SWIM_SPEED);
    }

    @WrapOperation(method = "collectEquipmentChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;equipmentHasChanged(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean nytheriacompat$callEquipmentChangeEvent(LivingEntity instance, ItemStack oldItem, ItemStack newItem, Operation<Boolean> original, @Local EquipmentSlot slot) {
        var result = original.call(instance, oldItem, newItem);
        if (result) {
            if (EventGate.has(LivingEquipmentChangeEvent.class)) NeoForge.EVENT_BUS.post(new LivingEquipmentChangeEvent((LivingEntity) (Object) this, slot, oldItem, newItem));
        }

        return result;
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInLava()Z", ordinal = 0))
    private void nytheriacompat$storeFluidType(CallbackInfo ci, @Share("fluidType") LocalRef<FluidType> fluidTypeRef) {
        fluidTypeRef.set(this.getMaxHeightFluidType());
    }

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFluidHeight(Lnet/minecraft/tags/TagKey;)D", ordinal = 1))
    private double nytheriacompat$tryUseFluidTypeHeight(LivingEntity instance, TagKey tagKey, Operation<Double> original, @Share("fluidType") LocalRef<FluidType> fluidTypeRef) {
        var fluidType = fluidTypeRef.get();

        if (!fluidType.isAir()) {
            return instance.getFluidTypeHeight(fluidType);
        }

        return original.call(instance, tagKey);
    }

    @Definition(id = "getFluidJumpThreshold", method = "Lnet/minecraft/world/entity/LivingEntity;getFluidJumpThreshold()D")
    @Expression("? = this.getFluidJumpThreshold()")
    @Inject(method = "aiStep", at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER))
    private void nytheriacompat$storeFluidHeightAndJumpThresholds(CallbackInfo ci, @Local(ordinal = 3) double d3, @Local(ordinal = 4) double d4, @Share("fluidHeight") LocalDoubleRef fluidHeightRef, @Share("fluidJumpThreshold") LocalDoubleRef fluidJumpThresholdRef) {
        fluidHeightRef.set(d3);
        fluidJumpThresholdRef.set(d4);
    }

    // like this
    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onGround()Z", ordinal = 2))
    private void nytheriacompat$handleFluidTypeJump(CallbackInfo ci, @Share("fluidType") LocalRef<FluidType> fluidTypeRef, @Share("fluidHeight") LocalDoubleRef fluidHeightRef, @Share("fluidJumpThreshold") LocalDoubleRef fluidJumpThresholdRef) {
        var fluidType = fluidTypeRef.get();
        if (fluidType == null)
            fluidType = this.getMaxHeightFluidType();

        if (!(fluidType.isAir() || this.onGround() && !(fluidHeightRef.get() > fluidJumpThresholdRef.get()))) {
            this.jumpInFluid(fluidType);
        }
    }

    @Definition(id = "noJumpDelay", field = "Lnet/minecraft/world/entity/LivingEntity;noJumpDelay:I")
    @Expression("this.noJumpDelay == 0")
    @ModifyExpressionValue(method = "aiStep", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$preventJumpGroundIfInFluid(boolean original, @Share("fluidType") LocalRef<FluidType> fluidTypeRef, @Share("fluidHeight") LocalDoubleRef fluidHeightRef, @Share("fluidJumpThreshold") LocalDoubleRef fluidJumpThresholdRef) {
        var fluidType = fluidTypeRef.get();
        if (fluidType == null)
            fluidType = this.getMaxHeightFluidType();

        if (fluidType.isAir() || this.onGround() && !(fluidHeightRef.get() > fluidJumpThresholdRef.get())) {
            return original;
        }

        return false;
    }

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;jumpInLiquid(Lnet/minecraft/tags/TagKey;)V"))
    private void nytheriacompat$checkIfFluidTypeIsActuallyType(LivingEntity instance, TagKey<Fluid> fluidTag, Operation<Void> original, @Share("fluidType") LocalRef<FluidType> fluidTypeRef) {
        if (fluidTag == FluidTags.WATER) {
            if (instance.getFluidHeight(FluidTags.WATER) > 0) {
                original.call(instance, fluidTag);
            } else {
                instance.jumpInFluid(NeoForgeMod.WATER_TYPE.value());
            }
        } else if (fluidTag == FluidTags.LAVA) {
            if (instance.getFluidHeight(FluidTags.LAVA) > 0) {
                original.call(instance, fluidTag);
            } else {
                instance.jumpInFluid(NeoForgeMod.LAVA_TYPE.value());
            }
        } else {
            original.call(instance, fluidTag);
        }
    }

    @Definition(id = "itemStack", local = @Local(type = ItemStack.class))
    @Definition(id = "is", method = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z")
    @Definition(id = "ELYTRA", field = "Lnet/minecraft/world/item/Items;ELYTRA:Lnet/minecraft/world/item/Item;")
    @Expression("itemStack.is(ELYTRA)")
    @WrapOperation(method = "updateFallFlying", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$checkCanElytraFly(ItemStack instance, Item item, Operation<Boolean> original) {
        return original.call(instance, item) || (Scope.isNeoObject(instance.getItem()) && instance.canElytraFly((LivingEntity) (Object) this));
    }

    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ElytraItem;isFlyEnabled(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean nytheriacompat$tryHandleFly(ItemStack elytraStack, Operation<Boolean> original, @Local LocalBooleanRef flag) {
        if (Scope.isNeoObject(elytraStack.getItem()) && elytraStack.getItem() != Items.ELYTRA && elytraStack.canElytraFly((LivingEntity) (Object) this)) {
            flag.set(elytraStack.elytraFlightTick((LivingEntity) (Object) this, this.fallFlyTicks));
            return false;
        }

        return original.call(elytraStack);
    }

    @Inject(method = "updateUsingItem", at = @At("HEAD"))
    private void nytheriacompat$callItemUseTickEvent(ItemStack usingItem, CallbackInfo ci) {
        if (!usingItem.isEmpty())
            this.useItemRemaining = EventHooks.onItemUseTick((LivingEntity) (Object) this, usingItem, this.getUseItemRemainingTicks());
    }

    // NeoForge: "if (--this.useItemRemaining <= 0 && ...) completeUsingItem()", vanilla compares with == 0, so an item with use duration 0 would never complete
    @Inject(method = "updateUsingItem", at = @At("TAIL"))
    private void nytheriacompat$completeNonPositiveUse(ItemStack usingItem, CallbackInfo ci) {
        if (this.useItemRemaining < 0 && !this.level().isClientSide && Scope.isNeoObject(usingItem.getItem()) && !usingItem.useOnRelease())
            this.completeUsingItem();
    }

    @Inject(method = "startUsingItem", at = @At("HEAD"))
    private void nytheriacompat$storeCurrentUseItem(InteractionHand hand, CallbackInfo ci, @Share("useItem") LocalRef<ItemStack> useItemRef) {
        useItemRef.set(this.useItem);
    }

    @WrapOperation(method = "startUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int nytheriacompat$tryStartUsingItem(ItemStack instance, LivingEntity entity, Operation<Integer> original, @Cancellable CallbackInfo ci, @Local(argsOnly = true) InteractionHand hand, @Share("useItem") LocalRef<ItemStack> useItemRef) {
        int duration = EventHooks.onItemUseStart(entity, instance, hand, original.call(instance, entity));

        if (duration < 0) {
            ci.cancel();
            this.useItem = useItemRef.get();
            return 0;
        }

        return duration;
    }

    @WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack nytheriacompat$tryFinishUsingItem(ItemStack instance, Level level, LivingEntity livingEntity, Operation<ItemStack> original) {
        return EventHooks.onItemUseFinish(livingEntity, instance.copy(), livingEntity.getUseItemRemainingTicks(), original.call(instance, level, livingEntity));
    }

    @WrapOperation(method = "releaseUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;releaseUsing(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V"))
    private void nytheriacompat$checkShouldStopUsingItem(ItemStack instance, Level level, LivingEntity livingEntity, int timeLeft, Operation<Void> original) {
        if (!EventHooks.onUseItemStop(livingEntity, instance, timeLeft)) {
            ItemStack copy = livingEntity instanceof Player ? instance.copy() : null;
            original.call(instance, level, livingEntity, timeLeft);

            if (copy != null && instance.isEmpty()) {
                EventHooks.onPlayerDestroyItem((Player) livingEntity, copy, livingEntity.getUsedItemHand());
            }
        }
    }

    @Definition(id = "item", local = @Local(type = Item.class))
    @Definition(id = "getUseAnimation", method = "Lnet/minecraft/world/item/Item;getUseAnimation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/UseAnim;")
    @Definition(id = "BLOCK", field = "Lnet/minecraft/world/item/UseAnim;BLOCK:Lnet/minecraft/world/item/UseAnim;")
    @Expression("item.getUseAnimation(?) != BLOCK")
    @ModifyExpressionValue(method = "isBlocking", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$checkCanPerformAction(boolean original) {
        return original && !(Scope.isNeoObject(this.useItem.getItem()) && this.useItem.canPerformAction(ItemAbilities.SHIELD_BLOCK));
    }

    @Override
    public boolean removeEffectsCuredBy(EffectCure cure) {
        if (this.level().isClientSide)
            return false;

        boolean ret = false;
        Iterator<MobEffectInstance> itr = this.activeEffects.values().iterator();

        while (itr.hasNext()) {
            MobEffectInstance effect = itr.next();

            if (effect.neoforge$getCures().contains(cure) && !EventHooks.onEffectRemoved((LivingEntity) (Object) this, effect, cure)) {
                this.onEffectRemoved(effect);
                itr.remove();
                ret = true;
                this.effectsDirty = true;
            }
        }

        return ret;
    }

    @Override
    public boolean shouldRiderFaceForward(Player player) {
        return (Object) this instanceof Pig;
    }

    @ModifyReturnValue(method = "canDisableShield", at = @At("RETURN"))
    private boolean nytheriacompat$checkCanDisableShield(boolean original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(this.getMainHandItem().getItem().getClass(), IItemExtension.class, "canDisableShield", ItemStack.class, ItemStack.class, LivingEntity.class, LivingEntity.class)) {
            return this.getMainHandItem().canDisableShield(this.useItem, (LivingEntity) (Object) this, (LivingEntity) (Object) this);
        }

        return original;
    }
}
