// TRACKED HASH: debf6874a4415fcbb527c106a281c5bd27a0b454
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import dev.nytheria.compat.hook.EventGate;
import dev.nytheria.compat.hook.Scope;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.world.item.ItemStackInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

@Mixin(ItemStack.class)
public abstract class ItemStackInject implements MutableDataComponentHolder, IItemStackExtension, ItemStackInjection {

    @Shadow public abstract Item getItem();
    @Shadow public abstract InteractionResult useOn(UseOnContext context);
    @Shadow public abstract boolean isEmpty();
    @Shadow @Final private PatchedDataComponentMap components;

    @Shadow
    public abstract ItemEnchantments getEnchantments();

    @Shadow
    public abstract boolean is(Item item);

    @Shadow
    public abstract Rarity getRarity();

    @Shadow
    public abstract void hurtAndBreak(int damage, ServerLevel level, @Nullable ServerPlayer player, Consumer<Item> onBreak);

    public ItemStackInject(ItemLike item, int count) {}

    @Unique
    private Function<UseOnContext, InteractionResult> nytheriacompat$callback;

    // Nytheria Compat: NeoForge returns CommonHooks.onPlaceItemIntoWorld here on the server, which skips the vanilla body (and every other mod's
    //  mixin into it). The vanilla body runs instead; the placement handling of onPlaceItemIntoWorld is done around its Item#useOn call
    //  (nytheriacompat$tryUseOnCallback / nytheriacompat$placeItemIntoWorld).
    @Override
    public InteractionResult onItemUseFirst(UseOnContext context) {
        if (EventGate.has(UseItemOnBlockEvent.class)) {
            var e = NeoForge.EVENT_BUS.post(new UseItemOnBlockEvent(context, UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK));
            if (e.isCanceled()) return e.getCancellationResult().result();
        }
        // Nytheria Compat: set after the event and always cleared again (a cancelled event or an exception used to leave it set, so the
        //  next useOn of this stack skipped ITEM_AFTER_BLOCK and the placement and called onItemUseFirst instead of Item#useOn).
        this.nytheriacompat$callback = c -> this.getItem().onItemUseFirst((ItemStack) (Object) this, c);
        try {
            return this.useOn(context);
        } finally {
            this.nytheriacompat$callback = null;
        }
    }

    @WrapOperation(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult nytheriacompat$tryUseOnCallback(Item instance, UseOnContext context, Operation<InteractionResult> original) {
        if (nytheriacompat$callback != null) {
            // Nytheria Compat: cleared before the call, so a useOn of this stack from inside onItemUseFirst takes the normal path.
            var callback = nytheriacompat$callback;
            nytheriacompat$callback = null;

            return callback.apply(context);
        }

        if (context.getLevel().isClientSide()) {
            return original.call(instance, context);
        }

        return nytheriacompat$placeItemIntoWorld(instance, context, original);
    }

    /**
     * Nytheria Compat: {@link CommonHooks#onPlaceItemIntoWorld} around the {@code Item#useOn} call of the vanilla body (logical server only):
     * block snapshots are captured during the call (not for buckets), {@code PlayerDestroyItemEvent} fires if the stack was used up,
     * and for a result that consumes the action the (multi) place event decides; when it is cancelled the blocks are restored, the stack
     * keeps its pre-use count and the result is FAIL, so the vanilla body awards no stat. Otherwise the vanilla body awards it as usual.
     * <p>
     * All per-call state is local, so a nested {@code useOn} (an item whose {@code useOn} uses another stack) gets its own: it only takes
     * the snapshots captured after it started, leaves the outer call's to it and switches capturing back to what it found. Capturing is
     * switched back also when {@code Item#useOn} or a listener throws.
     */
    @Unique
    private InteractionResult nytheriacompat$placeItemIntoWorld(Item item, UseOnContext context, Operation<InteractionResult> original) {
        ItemStack itemstack = context.getItemInHand();
        Level level = context.getLevel();
        Player player = context.getPlayer();

        boolean capture = !(item instanceof BucketItem) && nytheriacompat$needsPlaceCapture(item, context);
        boolean destroyEvent = EventGate.has(PlayerDestroyItemEvent.class);
        if (!capture && !destroyEvent) {
            return original.call(item, context);
        }

        int size = itemstack.getCount();
        DataComponentMap components = itemstack.getComponents();
        ItemStack copy = itemstack.copy();

        boolean wasCapturing = level.nytheriacompat$getCapturingBlockSnapshots();
        ArrayList<BlockSnapshot> captured = level.nytheriacompat$getCapturedBlockSnapshots();
        // Not nested: everything in the list is ours, like onPlaceItemIntoWorld. Nested in another capture: only what is added from here on.
        int start = wasCapturing ? captured.size() : 0;

        InteractionResult ret;
        List<BlockSnapshot> blockSnapshots = List.of();
        try {
            if (capture) level.nytheriacompat$setCapturingBlockSnapshots(true);
            try {
                ret = original.call(item, context);
                if (destroyEvent && itemstack.isEmpty())
                    EventHooks.onPlayerDestroyItem(player, copy, context.getHand());
            } finally {
                if (capture) {
                    // Off while the events fire and the blocks are restored or updated, as in onPlaceItemIntoWorld.
                    level.nytheriacompat$setCapturingBlockSnapshots(false);
                    // Taken off the level also on an exception, so they are never left for the next capture.
                    List<BlockSnapshot> own = captured.subList(Math.min(start, captured.size()), captured.size());
                    blockSnapshots = new ArrayList<>(own);
                    own.clear();
                }
            }

            if (capture && ret.consumesAction()) {
                ret = CommonHooks.nytheriacompat$firePlaceEvents(context, itemstack, item, size, components, blockSnapshots, ret, false);
            }
        } finally {
            if (capture) level.nytheriacompat$setCapturingBlockSnapshots(wasCapturing);
        }

        return ret;
    }

    /**
     * Nytheria Compat: capturing defers the neighbour updates of every block the item sets until {@code Item#useOn} ends, so it is on only
     * if a listener of the (multi) place event could get an event it needs: the item or the clicked block is from a NeoForge mod
     * (Astral's altar and Void Titan procedures), or the placer carries an effect of one (Cataclysm's {@code onPlaceBlock} cancels
     * every placement by an entity with its effect). Every other use runs as on plain Fabric.
     */
    @Unique
    private static boolean nytheriacompat$needsPlaceCapture(Item item, UseOnContext context) {
        // EntityMultiPlaceEvent extends EntityPlaceEvent: has() counts the listeners of the superclass, so this covers both events.
        if (!EventGate.has(BlockEvent.EntityMultiPlaceEvent.class)) return false;
        if (Scope.isNeoObject(item) || Scope.isNeoObject(context.getLevel().getBlockState(context.getClickedPos()).getBlock())) return true;

        if (context.getPlayer() != null) {
            for (Holder<MobEffect> effect : context.getPlayer().getActiveEffectsMap().keySet()) {
                var key = effect.unwrapKey();
                if (key.isPresent() && Scope.isNeoObject(key.get().location())) return true;
            }
        }

        return false;
    }

    // Nytheria Compat: Ignore wrapEncodingExceptions

    // Nytheria Compat: the item hooks below ask IItemExtension, which declares getMaxStackSize, isDamaged, getDamage, setDamage, getMaxDamage and
    //  getDefaultAttributeModifiers as defaults (their bodies are vanilla's); against Item.class every one of the checks was true for every item.

    @WrapOperation(method = "setDamageValue", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    private <T> Object trySetItemDamage(ItemStack instance, DataComponentType<? super T> component, T value, Operation<Object> original) {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverride(getItem().getClass(), IItemExtension.class, "setDamage", ItemStack.class, int.class)) {
            getItem().setDamage((ItemStack) (Object) this, (Integer) value);
            return null;
        }

        return original.call(instance, component, value);
    }

    @Override
    public void hurtAndBreak(int damage, ServerLevel level, @Nullable LivingEntity entity, Consumer<Item> onBreak) {
        this.hurtAndBreak(damage, level, null, onBreak);
    }

    @Definition(id = "withStyle", method = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;")
    @Definition(id = "getRarity", method = "Lnet/minecraft/world/item/ItemStack;getRarity()Lnet/minecraft/world/item/Rarity;")
    @Definition(id = "color", method = "Lnet/minecraft/world/item/Rarity;color()Lnet/minecraft/ChatFormatting;")
    @Expression("?.withStyle(this.getRarity().color())")
    @WrapOperation(
        method = {"getTooltipLines", "getDisplayName"},
        at = @At(value = "MIXINEXTRAS:EXPRESSION"),
        expect = 2
    )
    private MutableComponent nytheriacompat$applyRarityStyleModifier(
        MutableComponent instance, ChatFormatting chatFormatting, Operation<MutableComponent> original
    ) {
        var rarity = this.getRarity();
        if (rarity.nytheriacompat$hasCustomStyleModifier()) {
            return instance.withStyle(rarity.getStyleModifier());
        }
        return original.call(instance, chatFormatting);
    }

    // Nytheria Compat: fired here instead of through Fabric's ItemTooltipCallback, which has no player argument. Like NeoForge,
    //  only for the regular tooltip (second return; the first one returns the empty list of a hidden tooltip).
    @ModifyReturnValue(method = "getTooltipLines", at = @At(value = "RETURN", ordinal = 1))
    private List<Component> nytheriacompat$onItemTooltip(List<Component> original, Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag) {
        EventHooks.onItemTooltip((ItemStack) (Object) this, player, original, tooltipFlag, tooltipContext);
        return original;
    }

    // Nytheria Compat: okay seriously what's the point of this?
    @Override
    public ItemEnchantments getTagEnchantments() {
        return this.getEnchantments();
    }

    @TargetHandler(mixin = "io.github.fabricators_of_create.porting_lib.tool.mixin.ItemStackMixin", name = "canPerformAction")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, order = 1050)
    private void nytheriacompat$checkCanPerformActionForge(io.github.fabricators_of_create.porting_lib.tool.ItemAbility toolAction, CallbackInfoReturnable<Boolean> cir) {
        var forgeToolAction = ItemAbility.nytheriacompat$getNullable(toolAction.name());
        if (forgeToolAction != null && Scope.isNeoObject(this.getItem()) && this.canPerformAction(forgeToolAction)) {
            cir.setReturnValue(true);
        }
    }
}
