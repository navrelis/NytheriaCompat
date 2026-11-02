package dev.nytheria.compat.hook;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * When can a listener of an event have any effect? Each method here answers that for one event, from the bytecode of the
 * handlers (see {@code tools/trim/event-need.tsv}). A hook asks it right after {@link EventGate#has}: the gate says a listener
 * exists, and for these events one always does, this says the listener would not ignore the event.
 *
 * <p>The tests allocate nothing. An effect is looked up by a {@link DeferredHolder} like the handlers do; without the mod it
 * matches nothing. An item test is wider than the handler's single item: any item of a NeoForge mod.
 */
public final class Need {
    private static final DeferredHolder<MobEffect, MobEffect> STUN = effect("cataclysm", "stun");
    private static final DeferredHolder<MobEffect, MobEffect> GHOST_FORM = effect("cataclysm", "ghost_form");
    private static final DeferredHolder<MobEffect, MobEffect> GHOST_SICKNESS = effect("cataclysm", "ghost_sickness");
    private static final DeferredHolder<MobEffect, MobEffect> WETNESS = effect("cataclysm", "wetness");
    private static final DeferredHolder<MobEffect, MobEffect> ABYSSAL_FEAR = effect("cataclysm", "abyssal_fear");
    private static final DeferredHolder<MobEffect, MobEffect> THORNY = effect("astral_dimension", "thorny");
    private static final ResourceKey<DamageType> MALEDICTIO_SAGITTA = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("cataclysm", "maledictio_sagitta"));

    private Need() {
    }

    private static DeferredHolder<MobEffect, MobEffect> effect(String namespace, String path) {
        return DeferredHolder.create(Registries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static boolean neo(ItemStack stack) {
        return Scope.isNeoObject(stack.getItem());
    }

    /** Curios CuriosEventHandler#entityConstructing returns unless the entity is a LivingEntity. */
    public static boolean entityConstructing(Entity entity) {
        return entity instanceof LivingEntity;
    }

    /** Curios CuriosEventHandler#tick returns unless the entity is a LivingEntity. */
    public static boolean entityTickPost(Entity entity) {
        return entity instanceof LivingEntity;
    }

    /** Curios CuriosEventHandler#playerStartTracking wants a ServerPlayer tracking a LivingEntity. */
    public static boolean startTracking(Player player, Entity target) {
        return player instanceof ServerPlayer && target instanceof LivingEntity;
    }

    /** Cataclysm onShieldDamage: its damage type, or its shields in use. Astral PlayerBlocksWithRoseShield: the shield in a hand. */
    public static boolean shieldBlock(LivingEntity blocker, DamageSource source) {
        return source.is(MALEDICTIO_SAGITTA) || neo(blocker.getUseItem()) || neo(blocker.getMainHandItem()) || neo(blocker.getOffhandItem());
    }

    /** Cataclysm onLivingAttack: wetness, ghost form, or cursium leggings. */
    public static boolean incomingDamage(LivingEntity entity) {
        return entity.hasEffect(WETNESS) || entity.hasEffect(GHOST_FORM) || neo(entity.getItemBySlot(EquipmentSlot.LEGS));
    }

    /** Cataclysm BlockHeal cancels with abyssal fear. */
    public static boolean livingHeal(LivingEntity entity) {
        return entity.hasEffect(ABYSSAL_FEAR);
    }

    /** Cataclysm onLivingSetTargetEvent: a target wearing the ignitium helmet. */
    public static boolean changeTarget(LivingEntity target) {
        return target != null && neo(target.getItemBySlot(EquipmentSlot.HEAD));
    }

    /** Cataclysm onLivingFall: cursium boots, or a player (hook falling attachment). */
    public static boolean livingFall(LivingEntity entity) {
        return entity instanceof Player || neo(entity.getItemBySlot(EquipmentSlot.FEET));
    }

    /** Cataclysm onLivingJump: stun. Astral ThornyOnEffectActiveTick: thorny. */
    public static boolean livingJump(LivingEntity entity) {
        return entity.hasEffect(STUN) || entity.hasEffect(THORNY);
    }

    /** Cataclysm KnockbackEvent: Royal_Draugr_Entity. */
    public static boolean livingKnockBack(LivingEntity entity) {
        return Scope.isNeoObject((Entity) entity);
    }

    /** Cataclysm onUseItem: stun or ghost form. onUseTick, onStopUsing: its shields. */
    public static boolean useItem(LivingEntity entity, ItemStack item) {
        return entity.hasEffect(STUN) || entity.hasEffect(GHOST_FORM) || neo(item);
    }

    /** Cataclysm preventEffectRemoval: ghost sickness only. */
    public static boolean effectRemove(Holder<MobEffect> effect) {
        return GHOST_SICKNESS.equals(effect);
    }

    /** Astral onItemDropped (three handlers): its backpacks and diary. */
    public static boolean itemToss(Item item) {
        return Scope.isNeoObject(item);
    }

    /** Cataclysm onPlayerAttack: stun or ghost form. */
    public static boolean attackEntity(Player player) {
        return player.hasEffect(STUN) || player.hasEffect(GHOST_FORM);
    }

    /** Cataclysm onPlayerInteract3, onPlayerInteract5 and onPlayerLeftClick: stun. */
    public static boolean interact(Player player) {
        return player.hasEffect(STUN);
    }

    /** Cataclysm onBreakBlock: stun. Astral GlowstoneBlockBrokenByPlayer: glowstone. */
    public static boolean blockBreak(Player player, BlockState state) {
        return player.hasEffect(STUN) || state.is(Blocks.GLOWSTONE);
    }
}
