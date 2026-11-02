// TRACKED HASH: da42f0fcd542552388a5aff060abf470c54f9f10
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.player;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.hook.EventGate;
import dev.nytheria.compat.injections.world.entity.player.PlayerInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;
import dev.nytheria.compat.hook.Scope;


@Mixin(Player.class)
public abstract class PlayerInject extends LivingEntity implements IPlayerExtension, PlayerInjection {
    @Shadow public abstract float getDestroySpeed(BlockState state);
    @Shadow public abstract void resetAttackStrengthTicker();
    @Shadow public abstract @Nullable ItemEntity drop(ItemStack itemStack, boolean includeThrowerName);

    @CreateStatic
    private static final String PERSISTED_NBT_TAG = PlayerInjection.PERSISTED_NBT_TAG;


    protected PlayerInject(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void nytheriacompat$playerTickEnd(CallbackInfo ci) {
        EventHooks.firePlayerTickPost((Player) (Object) this);
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$checkShouldDie(DamageSource damageSource, CallbackInfo ci) {
        if (this.nytheriacompat$postVanillaLivingDeath(damageSource)) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity nytheriacompat$checkPlayerTossEvent(Player instance, ItemStack itemStack, boolean dropAround, boolean includeName, Operation<ItemEntity> original) {
        return CommonHooks.nytheriacompat$onPlayerTossEvent(instance, itemStack, () -> original.call(instance, itemStack, dropAround, includeName));
    }

    @Override
    public float getDigSpeed(BlockState blockState, @Nullable BlockPos blockPos) {
        return this.getDestroySpeed(blockState);
    }

    @Override
    public boolean hasCorrectToolForDrops(BlockState state, Level level, BlockPos pos) {
        return EventHooks.doPlayerHarvestCheck((Player) (Object) this, state, level, pos);
    }

    @Inject(method = "hurt", at = @At("HEAD"))
    private void nytheriacompat$storeOriginalAmount(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir, @Share("originalAmount") LocalFloatRef amountRef) {
        amountRef.set(amount);
    }

    @Definition(id = "useItem", field = "Lnet/minecraft/world/entity/player/Player;useItem:Lnet/minecraft/world/item/ItemStack;")
    @Definition(id = "is", method = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z")
    @Definition(id = "SHIELD", field = "Lnet/minecraft/world/item/Items;SHIELD:Lnet/minecraft/world/item/Item;")
    @Expression("this.useItem.is(SHIELD)")
    @ModifyExpressionValue(method = "hurtCurrentlyUsedShield", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$checkCanBeShield(boolean original) {
        return original || (Scope.isNeoObject(this.useItem.getItem()) && this.useItem.canPerformAction(ItemAbilities.SHIELD_BLOCK));
    }

    // Nytheria Compat: We're a completely different file from LivingEntity and I still hate this system.

    // Nytheria Compat: the amount in actuallyHurt stays the one that counts, see LivingEntityInject.
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float nytheriacompat$tryReduceWithArmorAbsorb(Player instance, DamageSource damageSource, float damageAmount, Operation<Float> original) {
        DamageContainer container = this.nytheriacompat$getDamageContainers().peek();

        var reduced = original.call(instance, damageSource, damageAmount);
        container.setReduction(DamageContainer.Reduction.ARMOR, container.getNewDamage() - reduced);

        return reduced;
    }

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private void nytheriacompat$callLivingPreDamageEvent(DamageSource damageSource, float damageAmount, CallbackInfo ci, @Share("damage") LocalFloatRef damageRef) {
        DamageContainer container = this.nytheriacompat$getDamageContainers().peek();
        container.setNewDamage(damageAmount);
        damageRef.set(CommonHooks.onLivingDamagePre(this, container));
    }

    // Nytheria Compat: the event is posted before absorption like in NeoForge. Without a listener the result is vanilla's.
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float nytheriacompat$doAbsorptionModification(float a, float b, Operation<Float> original, @Share("damage") LocalFloatRef damageRef) {
        DamageContainer container = this.nytheriacompat$getDamageContainers().peek();
        container.setReduction(DamageContainer.Reduction.ABSORPTION, Math.min(damageRef.get(), this.getAbsorptionAmount()));
        float vanilla = original.call(a, b);
        return EventGate.has(LivingDamageEvent.Pre.class) ? container.getNewDamage() : vanilla;
    }

    @Definition(id = "damageAmount", local = @Local(type = float.class, ordinal = 0, argsOnly = true))
    @Definition(id = "f", local = @Local(type = float.class, ordinal = 1))
    @Expression("f - damageAmount")
    @ModifyExpressionValue(method = "actuallyHurt", at = @At("MIXINEXTRAS:EXPRESSION"))
    private float nytheriacompat$useAbsorbedDamage(float original, @Share("damage") LocalFloatRef damageRef) {
        if (!EventGate.has(LivingDamageEvent.Pre.class)) return original;
        return Math.min(damageRef.get(), this.nytheriacompat$getDamageContainers().peek().getReduction(DamageContainer.Reduction.ABSORPTION));
    }

    @ModifyArg(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setAbsorptionAmount(F)V"))
    private float nytheriacompat$clampAbsorptionAmount(float absorptionAmount) {
        return Math.max(absorptionAmount, 0);
    }

    // Nytheria Compat: Porting Lib cancels actuallyHurt for damage <= 0 (a fully blocked hit), so a TAIL inject would not run there.
    @WrapMethod(method = "actuallyHurt")
    private void nytheriacompat$callLivingPostDamageEvent(DamageSource damageSource, float damageAmount, Operation<Void> original) {
        // Nytheria Compat: without hurt there is no container of hurt to use, see LivingEntityInject.
        boolean outsideHurt = this.nytheriacompat$getDamageContainers().isEmpty();
        if (outsideHurt) this.nytheriacompat$getDamageContainers().push(new DamageContainer(damageSource, damageAmount));
        this.nytheriacompat$getDamageContainers().peek().setNewDamage(damageAmount);
        original.call(damageSource, damageAmount);

        if (!this.isInvulnerableTo(damageSource)) {
            CommonHooks.onLivingDamagePost(this, this.nytheriacompat$getDamageContainers().peek());
        }
        if (outsideHurt) this.nytheriacompat$getDamageContainers().pop();
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$checkPlayerTargetAttack(Entity target, CallbackInfo ci) {
        if (!CommonHooks.onPlayerAttackTarget((Player) (Object) this, target))
            ci.cancel();
    }

    // Nytheria Compat: Critical hit implemented by Porting Lib

    @WrapWithCondition(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;resetAttackStrengthTicker()V"))
    private boolean nytheriacompat$avoidStrengthTickerReset(Player instance) {
        return false;
    }

    @Definition(id = "entity", local = @Local(type = Entity.class, argsOnly = true))
    @Definition(id = "EnderDragonPart", type = EnderDragonPart.class)
    @Expression("entity instanceof EnderDragonPart")
    @WrapOperation(method = "attack", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nytheriacompat$trySetAttackedMultipartEntity(Object object, Operation<Boolean> original, @Local(ordinal = 1) LocalRef<Entity> entity) {
        if (original.call(object))
            return true;

        if (object instanceof PartEntity<?> partEntity) {
            entity.set(partEntity.getParent());
        }

        return false;
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void nytheriacompat$resetStrengthTicker(Entity target, CallbackInfo ci) {
        if (target.isAttackable() && !target.skipAttackInteraction(this)) {
            this.resetAttackStrengthTicker();
        }
    }

    @WrapOperation(method = "disableShield", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Items;SHIELD:Lnet/minecraft/world/item/Item;"))
    private Item nytheriacompat$useUseItemItem(Operation<Item> original) {
        var item = original.call();

        if (item == Items.SHIELD) {
            return this.getUseItem().getItem();
        }

        return item;
    }

    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void nytheriacompat$callPlayerWakeup(boolean wakeImmediately, boolean updateLevelForSleepingPlayers, CallbackInfo ci) {
        EventHooks.onPlayerWakeup((Player) (Object) this, wakeImmediately, updateLevelForSleepingPlayers);
    }

    @WrapOperation(method = "tryToStartFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean nytheriacompat$checkCanElytraFly(ItemStack instance, Item item, Operation<Boolean> original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(instance.getItem().getClass(), IItemExtension.class, "canElytraFly", ItemStack.class, LivingEntity.class)) {
            return instance.canElytraFly(this);
        }

        return original.call(instance, item);
    }

    // TODO: step sounds

    // TODO: how tf
//    @Definition(id = "experienceLevel", field = "Lnet/minecraft/world/entity/player/Player;experienceLevel:I")
//    @Expression("this.experienceLevel = this.experienceLevel - ?")
//    @Redirect(method = "onEnchantmentPerformed", at = @At("MIXINEXTRAS:EXPRESSION"))
//    private void nytheriacompat$giveNegativeExperienceLevels() {
//
//    }

    @WrapOperation(method = "isScoping", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean nytheriacompat$checkHasSpyglassScopeAbility(ItemStack instance, Item item, Operation<Boolean> original) {
        return original.call(instance, item) || (Scope.isNeoObject(instance.getItem()) && instance.canPerformAction(ItemAbilities.SPYGLASS_SCOPE));
    }

}
