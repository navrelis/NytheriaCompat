/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on NeoForge by NeoForged, as ported by KiltMC (see VENDORED.md). Licensed under the GNU LGPL 2.1.
 */

package net.neoforged.neoforge.event;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DynamicOps;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.ItemAbility;
import dev.nytheria.compat.hook.EventGate;
import dev.nytheria.compat.hook.Need;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IFluidStateExtension;
import net.neoforged.neoforge.common.extensions.IOwnedSpawner;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.InsertableLinkedOpenCustomHashSet;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.brewing.PotionBrewEvent;
import net.neoforged.neoforge.event.enchanting.EnchantmentLevelSetEvent;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck;
import net.neoforged.neoforge.event.entity.living.MobSplitEvent;
import net.neoforged.neoforge.event.entity.living.SpawnClusterSizeEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent.AdvancementProgressEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent.AdvancementProgressEvent.ProgressType;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.ArrowNockEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PermissionsChangedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerFlyableFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerHeartTypeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.event.level.AlterGroundEvent;
import net.neoforged.neoforge.event.level.AlterGroundEvent.StateProvider;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityMultiPlaceEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.ModifyCustomSpawnersEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Player.BedSleepingProblem;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.AlterGroundDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EventHooks {
    public static boolean onMultiBlockPlace(@Nullable Entity entity, List<BlockSnapshot> blockSnapshots, Direction direction) {
        BlockSnapshot snap = blockSnapshots.get(0);
        BlockState placedAgainst = snap.getLevel().getBlockState(snap.getPos().relative(direction.getOpposite()));
        EntityMultiPlaceEvent event = new EntityMultiPlaceEvent(blockSnapshots, placedAgainst, entity);
        return (EventGate.has(EntityMultiPlaceEvent.class) && NeoForge.EVENT_BUS.post(event).isCanceled());
    }

    public static boolean onBlockPlace(@Nullable Entity entity, BlockSnapshot blockSnapshot, Direction direction) {
        BlockState placedAgainst = blockSnapshot.getLevel().getBlockState(blockSnapshot.getPos().relative(direction.getOpposite()));
        EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(blockSnapshot, placedAgainst, entity);
        return (EventGate.has(BlockEvent.EntityPlaceEvent.class) && NeoForge.EVENT_BUS.post(event).isCanceled());
    }

    public static NeighborNotifyEvent onNeighborNotify(Level level, BlockPos pos, BlockState state, EnumSet<Direction> notifiedSides, boolean forceRedstoneUpdate) {
        NeighborNotifyEvent event = new NeighborNotifyEvent(level, pos, state, notifiedSides, forceRedstoneUpdate);
        if (EventGate.has(NeighborNotifyEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event;
    }

    public static boolean doPlayerHarvestCheck(Player player, BlockState state, BlockGetter level, BlockPos pos) {
        // Call deprecated hasCorrectToolForDrops overload for a fallback value, in turn the non-deprecated overload calls this method
        boolean vanillaValue = player.hasCorrectToolForDrops(state);
        return nytheriacompat$doPlayerHarvestCheck(player, state, level, pos, vanillaValue);
    }

    public static boolean nytheriacompat$doPlayerHarvestCheck(Player player, BlockState state, BlockGetter level, BlockPos pos, boolean vanillaValue) {
        if (!EventGate.has(PlayerEvent.HarvestCheck.class)) return vanillaValue;
        PlayerEvent.HarvestCheck event = NeoForge.EVENT_BUS.post(new PlayerEvent.HarvestCheck(player, state, level, pos, vanillaValue));
        return event.canHarvest();
    }

    public static float getBreakSpeed(Player player, BlockState state, float original, BlockPos pos) {
        PlayerEvent.BreakSpeed event = new PlayerEvent.BreakSpeed(player, state, original, pos);
        return ((EventGate.has(PlayerEvent.BreakSpeed.class) && NeoForge.EVENT_BUS.post(event).isCanceled()) ? -1 : event.getNewSpeed());
    }

    public static void onPlayerDestroyItem(Player player, ItemStack stack, @Nullable InteractionHand hand) {
        if (EventGate.has(PlayerDestroyItemEvent.class)) NeoForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, stack, hand));
    }

    /**
     * Checks if the current position of the passed mob is valid for spawning, by firing {@link PositionCheck}.<br>
     * The default check is to perform the logical and of {@link Mob#checkSpawnRules} and {@link Mob#checkSpawnObstruction}.<br>
     * 
     * @param mob       The mob being spawned.
     * @param level     The level the mob will be added to, if successful.
     * @param spawnType The spawn type of the spawn.
     * @return True, if the position is valid, as determined by the contract of {@link PositionCheck}.
     * @see PositionCheck
     */
    public static boolean checkSpawnPosition(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType) {
        var event = new PositionCheck(mob, level, spawnType, null);
        if (EventGate.has(PositionCheck.class)) NeoForge.EVENT_BUS.post(event);
        if (event.getResult() == PositionCheck.Result.DEFAULT) {
            return mob.checkSpawnRules(level, spawnType) && mob.checkSpawnObstruction(level);
        }
        return event.getResult() == PositionCheck.Result.SUCCEED;
    }

    public static PositionCheck.Result nytheriacompat$checkSpawnPosition(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType) {
        var event = new PositionCheck(mob, level, spawnType, null);
        if (EventGate.has(PositionCheck.class)) NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }

    /**
     * Specialized variant of {@link #checkSpawnPosition} for spawners, as they have slightly different checks, and pass through the {@link BaseSpawner} to the event.
     * 
     * @see #checkSpawnPosition(Mob, ServerLevelAccessor, MobSpawnType)
     * @implNote See in-line comments about custom spawn rules.
     */
    public static boolean checkSpawnPositionSpawner(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType, SpawnData spawnData, BaseSpawner spawner) {
        var event = new PositionCheck(mob, level, spawnType, spawner);
        if (EventGate.has(PositionCheck.class)) NeoForge.EVENT_BUS.post(event);
        if (event.getResult() == PositionCheck.Result.DEFAULT) {
            // Spawners do not evaluate Mob#checkSpawnRules if any custom rules are present. This is despite the fact that these two methods do not check the same things.
            return (spawnData.getCustomSpawnRules().isPresent() || mob.checkSpawnRules(level, spawnType)) && mob.checkSpawnObstruction(level);
        }
        return event.getResult() == PositionCheck.Result.SUCCEED;
    }

    /**
     * Finalizes the spawn of a mob by firing the {@link FinalizeSpawnEvent} and calling {@link Mob#finalizeSpawn} with the result.
     * <p>
     * Mods should call this method in place of calling {@link Mob#finalizeSpawn}, unless calling super from within an override.
     * Vanilla calls to {@link Mob#finalizeSpawn} are replaced with calls to this method via coremod, so calls to this method will not show in an IDE.
     * <p>
     * When interfacing with this event, write all code as normal, and replace the call to {@link Mob#finalizeSpawn} with a call to this method.<p>
     * As an example, the following code block:
     * <code>
     * 
     * <pre>
     * var zombie = new Zombie(level);
     * zombie.finalizeSpawn(level, difficulty, spawnType, spawnData);
     * level.tryAddFreshEntityWithPassengers(zombie);
     * if (zombie.isAddedToLevel()) {
     *     // Do stuff with your new zombie
     * }
     * </pre>
     * 
     * </code>
     * Would become:
     * <code>
     * 
     * <pre>
     * var zombie = new Zombie(level);
     * EventHooks.finalizeMobSpawn(zombie, level, difficulty, spawnType, spawnData);
     * level.tryAddFreshEntityWithPassengers(zombie);
     * if (zombie.isAddedToLevel()) {
     *     // Do stuff with your new zombie
     * }
     * </pre>
     * 
     * </code>
     * The only code that changes is the {@link Mob#finalizeSpawn} call.
     * 
     * @param mob        The mob whose spawn is being finalized
     * @param level      The level the mob will be spawned in
     * @param difficulty The local difficulty at the position of the mob
     * @param spawnType  The type of spawn that is occuring
     * @param spawnData  Optional spawn data relevant to the mob being spawned
     * @return The SpawnGroupData from the finalize, or null if it was canceled. The return value of this method has no bearing on if the entity will be spawned
     * 
     * @see FinalizeSpawnEvent
     * @see Mob#finalizeSpawn(ServerLevelAccessor, DifficultyInstance, MobSpawnType, SpawnGroupData)
     * 
     * @apiNote Callers do not need to check if the entity's spawn was cancelled, as the spawn will be blocked by Forge.
     * 
     * @implNote Changes to the signature of this method must be reflected in the method redirector coremod.
     */
    @Nullable
    @SuppressWarnings("deprecation") // Call to deprecated Mob#finalizeSpawn is expected.
    public static SpawnGroupData finalizeMobSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        var event = new FinalizeSpawnEvent(mob, level, mob.getX(), mob.getY(), mob.getZ(), difficulty, spawnType, spawnData, null);
        if (EventGate.has(FinalizeSpawnEvent.class)) NeoForge.EVENT_BUS.post(event);

        if (!event.isCanceled()) {
            return mob.finalizeSpawn(level, event.getDifficulty(), event.getSpawnType(), event.getSpawnData());
        }

        return null;
    }

    /**
     * Finalizes the spawn of a mob by firing the {@link FinalizeSpawnEvent} and calling {@link Mob#finalizeSpawn} with the result.
     * <p>
     * This method is separate since mob spawners perform special finalizeSpawn handling when NBT data is present, but we still want to fire the event.
     * <p>
     * This overload is also the only way to pass through an {@link IOwnedSpawner} instance.
     * 
     * @param mob        The mob whose spawn is being finalized
     * @param level      The level the mob will be spawned in
     * @param difficulty The local difficulty at the position of the mob
     * @param spawnType  The type of spawn that is occuring
     * @param spawnData  Optional spawn data relevant to the mob being spawned
     * @param spawner    The spawner that is attempting to spawn the mob
     * @param def        If the spawner would normally call finalizeSpawn, regardless of the event
     */
    @SuppressWarnings("deprecation") // Call to deprecated Mob#finalizeSpawn is expected.
    public static FinalizeSpawnEvent finalizeMobSpawnSpawner(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, IOwnedSpawner spawner, boolean def) {
        var event = new FinalizeSpawnEvent(mob, level, mob.getX(), mob.getY(), mob.getZ(), difficulty, spawnType, spawnData, spawner.getOwner());
        if (EventGate.has(FinalizeSpawnEvent.class)) NeoForge.EVENT_BUS.post(event);

        if (!event.isCanceled() && def) {
            // Spawners only call finalizeSpawn under certain conditions, which are passed through as def.
            // Spawners also do not propagate the SpawnGroupData between spawns, so we ignore the result of Mob#finalizeSpawn
            mob.finalizeSpawn(level, event.getDifficulty(), event.getSpawnType(), event.getSpawnData());
        }

        return event;
    }

    // Nytheria Compat: Mod compatible variant
    public static SpawnGroupData nytheriacompat$finalizeMobSpawnSpawner(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, IOwnedSpawner spawner, boolean def, Operation<SpawnGroupData> original) {
        var event = new FinalizeSpawnEvent(mob, level, mob.getX(), mob.getY(), mob.getZ(), difficulty, spawnType, spawnData, spawner.getOwner());
        if (EventGate.has(FinalizeSpawnEvent.class)) NeoForge.EVENT_BUS.post(event);

        if (!event.isCanceled() && def) {
            // Spawners only call finalizeSpawn under certain conditions, which are passed through as def.
            // Spawners also do not propagate the SpawnGroupData between spawns, so we ignore the result of Mob#finalizeSpawn
            return original.call(mob, level, event.getDifficulty(), event.getSpawnType(), event.getSpawnData());
        }

        return event.getSpawnData();
    }

    /**
     * Called from {@link PhantomSpawner#tick} just before the spawn conditions for phantoms are evaluated.
     * Fires the {@link PlayerSpawnPhantomsEvent} and returns the event.
     * 
     * @param player The player for whom a spawn attempt is being made
     * @param level  The level of the player
     * @param pos    The block position of the player
     */
    public static PlayerSpawnPhantomsEvent firePlayerSpawnPhantoms(ServerPlayer player, ServerLevel level, BlockPos pos) {
        Difficulty difficulty = level.getCurrentDifficultyAt(pos).getDifficulty();
        var event = new PlayerSpawnPhantomsEvent(player, 1 + level.random.nextInt(difficulty.getId() + 1));
        if (EventGate.has(PlayerSpawnPhantomsEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event;
    }

    /**
     * Fires {@link MobDespawnEvent} and returns true if the default logic should be ignored.
     * 
     * @param entity The entity being despawned.
     * @return True if the event result is not {@link MobDespawnEvent.Result#DEFAULT}, and the vanilla logic should be ignored.
     */
    public static boolean checkMobDespawn(Mob mob) {
        MobDespawnEvent event = new MobDespawnEvent(mob, (ServerLevel) mob.level());
        if (EventGate.has(MobDespawnEvent.class)) NeoForge.EVENT_BUS.post(event);

        switch (event.getResult()) {
            case ALLOW -> mob.discard();
            case DEFAULT -> {}
            case DENY -> mob.setNoActionTime(0);
        }

        return event.getResult() != MobDespawnEvent.Result.DEFAULT;
    }

    public static int getItemBurnTime(ItemStack itemStack, int burnTime, @Nullable RecipeType<?> recipeType) {
        FurnaceFuelBurnTimeEvent event = new FurnaceFuelBurnTimeEvent(itemStack, burnTime, recipeType);
        if (EventGate.has(FurnaceFuelBurnTimeEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event.getBurnTime();
    }

    public static int getExperienceDrop(LivingEntity entity, Player attackingPlayer, int originalExperience) {
        LivingExperienceDropEvent event = new LivingExperienceDropEvent(entity, attackingPlayer, originalExperience);
        if ((EventGate.has(LivingExperienceDropEvent.class) && NeoForge.EVENT_BUS.post(event).isCanceled())) {
            return 0;
        }
        return event.getDroppedExperience();
    }

    /**
     * Fires {@link SpawnClusterSizeEvent} and returns the size as a result of the event.
     * <p>
     * Called in {@link NaturalSpawner#spawnCategoryForPosition} where {@link Mob#getMaxSpawnClusterSize()} would normally be called.
     * 
     * @param entity The entity whose max spawn cluster size is being queried.
     * 
     * @return The new spawn cluster size.
     */
    public static int getMaxSpawnClusterSize(Mob entity) {
        var event = new SpawnClusterSizeEvent(entity);
        if (EventGate.has(SpawnClusterSizeEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event.getSize();
    }

    public static Component getPlayerDisplayName(Player player, Component username) {
        PlayerEvent.NameFormat event = new PlayerEvent.NameFormat(player, username);
        if (EventGate.has(PlayerEvent.NameFormat.class)) NeoForge.EVENT_BUS.post(event);
        return event.getDisplayname();
    }

    public static Component getPlayerTabListDisplayName(Player player) {
        PlayerEvent.TabListNameFormat event = new PlayerEvent.TabListNameFormat(player);
        if (EventGate.has(PlayerEvent.TabListNameFormat.class)) NeoForge.EVENT_BUS.post(event);
        return event.getDisplayName();
    }

    public static BlockState fireFluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
        BlockEvent.FluidPlaceBlockEvent event = new BlockEvent.FluidPlaceBlockEvent(level, pos, liquidPos, state);
        if (EventGate.has(BlockEvent.FluidPlaceBlockEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event.getNewState();
    }

    public static ItemTooltipEvent onItemTooltip(ItemStack itemStack, @Nullable Player entityPlayer, List<Component> list, TooltipFlag flags, Item.TooltipContext context) {
        ItemTooltipEvent event = new ItemTooltipEvent(itemStack, entityPlayer, list, flags, context);
        if (EventGate.has(ItemTooltipEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event;
    }

    public static boolean onEntityStruckByLightning(Entity entity, LightningBolt bolt) {
        return EventGate.has(EntityStruckByLightningEvent.class) && NeoForge.EVENT_BUS.post(new EntityStruckByLightningEvent(entity, bolt)).isCanceled();
    }

    /**
     * @deprecated Use {@link #onItemUseStart(LivingEntity, ItemStack, InteractionHand, int) the hand sensitive version} as this version provides wrong hand information
     */
    @Deprecated(since = "1.21.5", forRemoval = true)
    public static int onItemUseStart(LivingEntity entity, ItemStack item, int duration) {
        return onItemUseStart(entity, item, entity.getItemInHand(InteractionHand.MAIN_HAND) == item ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, duration);
    }

    public static int onItemUseStart(LivingEntity entity, ItemStack item, InteractionHand hand, int duration) {
        if (!EventGate.has(LivingEntityUseItemEvent.Start.class)) return duration;
        var event = new LivingEntityUseItemEvent.Start(entity, item, hand, duration);
        return NeoForge.EVENT_BUS.post(event).isCanceled() ? -1 : event.getDuration();
    }

    public static int onItemUseTick(LivingEntity entity, ItemStack item, int duration) {
        if (!EventGate.has(LivingEntityUseItemEvent.Tick.class) || !Need.useItem(entity, item)) return duration;
        var event = new LivingEntityUseItemEvent.Tick(entity, item, duration);
        return NeoForge.EVENT_BUS.post(event).isCanceled() ? -1 : event.getDuration();
    }

    public static boolean onUseItemStop(LivingEntity entity, ItemStack item, int duration) {
        return EventGate.has(LivingEntityUseItemEvent.Stop.class) && Need.useItem(entity, item) && NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Stop(entity, item, duration)).isCanceled();
    }

    public static ItemStack onItemUseFinish(LivingEntity entity, ItemStack item, int duration, ItemStack result) {
        LivingEntityUseItemEvent.Finish event = new LivingEntityUseItemEvent.Finish(entity, item, duration, result);
        if (EventGate.has(LivingEntityUseItemEvent.Finish.class) && Need.useItem(entity, item)) NeoForge.EVENT_BUS.post(event);
        return event.getResultStack();
    }

    public static void onStartEntityTracking(Entity entity, Player player) {
        if (EventGate.has(PlayerEvent.StartTracking.class) && Need.startTracking(player, entity)) NeoForge.EVENT_BUS.post(new PlayerEvent.StartTracking(player, entity));
    }

    @Nullable
    public static BlockState onToolUse(BlockState originalState, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        BlockToolModificationEvent event = new BlockToolModificationEvent(originalState, context, itemAbility, simulate);
        return (EventGate.has(BlockToolModificationEvent.class) && NeoForge.EVENT_BUS.post(event).isCanceled()) ? null : event.getFinalState();
    }

    public static PlayLevelSoundEvent.AtEntity onPlaySoundAtEntity(Entity entity, Holder<SoundEvent> name, SoundSource category, float volume, float pitch) {
        PlayLevelSoundEvent.AtEntity event = new PlayLevelSoundEvent.AtEntity(entity, name, category, volume, pitch);
        if (EventGate.has(PlayLevelSoundEvent.AtEntity.class)) NeoForge.EVENT_BUS.post(event);
        return event;
    }

    public static int onItemExpire(ItemEntity entity) {
        ItemExpireEvent event = new ItemExpireEvent(entity);
        if (EventGate.has(ItemExpireEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event.getExtraLife();
    }

    /**
     * Called in {@link ItemEntity#playerTouch(Player)} before any other processing occurs.
     * <p>
     * Fires {@link ItemEntityPickupEvent.Pre} and returns the event.
     * 
     * @param itemEntity The item entity that a player collided with
     * @param player     The player that collided with the item entity
     */
    public static ItemEntityPickupEvent.Pre fireItemPickupPre(ItemEntity itemEntity, Player player) {
        ItemEntityPickupEvent.Pre event = new ItemEntityPickupEvent.Pre(player, itemEntity);
        if (EventGate.has(ItemEntityPickupEvent.Pre.class)) NeoForge.EVENT_BUS.post(event);
        return event;
    }

    /**
     * Called in {@link ItemEntity#playerTouch(Player)} after an item was successfully picked up.
     * <p>
     * Fires {@link ItemEntityPickupEvent.Post}.
     * 
     * @param itemEntity The item entity that a player collided with
     * @param player     The player that collided with the item entity
     * @param copy       A copy of the item entity's item stack before the pickup
     */
    public static void fireItemPickupPost(ItemEntity itemEntity, Player player, ItemStack copy) {
        if (EventGate.has(ItemEntityPickupEvent.Post.class)) NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Post(player, itemEntity, copy));
    }

    public static boolean canMountEntity(Entity entityMounting, Entity entityBeingMounted, boolean isMounting) {
        boolean isCanceled = (EventGate.has(EntityMountEvent.class) && NeoForge.EVENT_BUS.post(new EntityMountEvent(entityMounting, entityBeingMounted, entityMounting.level(), isMounting)).isCanceled());

        if (isCanceled) {
            entityMounting.absMoveTo(entityMounting.getX(), entityMounting.getY(), entityMounting.getZ(), entityMounting.yRotO, entityMounting.xRotO);
            return false;
        } else
            return true;
    }

    public static boolean onAnimalTame(Animal animal, Player tamer) {
        return EventGate.has(AnimalTameEvent.class) && NeoForge.EVENT_BUS.post(new AnimalTameEvent(animal, tamer)).isCanceled();
    }

    public static Either<BedSleepingProblem, Unit> canPlayerStartSleeping(ServerPlayer player, BlockPos pos, Either<BedSleepingProblem, Unit> vanillaResult) {
        CanPlayerSleepEvent event = new CanPlayerSleepEvent(player, pos, vanillaResult.left().orElse(null));
        if (EventGate.has(CanPlayerSleepEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event.getProblem() != null ? Either.left(event.getProblem()) : Either.right(Unit.INSTANCE);
    }

    public static void onPlayerWakeup(Player player, boolean wakeImmediately, boolean updateLevel) {
        if (EventGate.has(PlayerWakeUpEvent.class)) NeoForge.EVENT_BUS.post(new PlayerWakeUpEvent(player, wakeImmediately, updateLevel));
    }

    public static boolean onPlayerSpawnSet(Player player, ResourceKey<Level> levelKey, BlockPos pos, boolean forced) {
        return EventGate.has(PlayerSetSpawnEvent.class) && NeoForge.EVENT_BUS.post(new PlayerSetSpawnEvent(player, levelKey, pos, forced)).isCanceled();
    }

    public static void onPlayerClone(Player player, Player oldPlayer, boolean wasDeath) {
        if (EventGate.has(PlayerEvent.Clone.class)) NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(player, oldPlayer, wasDeath));
    }

    public static boolean onExplosionStart(Level level, Explosion explosion) {
        return EventGate.has(ExplosionEvent.Start.class) && NeoForge.EVENT_BUS.post(new ExplosionEvent.Start(level, explosion)).isCanceled();
    }

    public static void onExplosionDetonate(Level level, Explosion explosion, List<Entity> list, double diameter) {
        //Filter entities to only those who are effected, to prevent modders from seeing more then will be hurt.
        /* Enable this if we get issues with modders looping to much.
        Iterator<Entity> itr = list.iterator();
        Vec3 p = explosion.getPosition();
        while (itr.hasNext())
        {
            Entity e = itr.next();
            double dist = e.getDistance(p.xCoord, p.yCoord, p.zCoord) / diameter;
            if (e.isImmuneToExplosions() || dist > 1.0F) itr.remove();
        }
        */
        if (EventGate.has(ExplosionEvent.Detonate.class)) NeoForge.EVENT_BUS.post(new ExplosionEvent.Detonate(level, explosion, list));
    }

    /**
     * To be called when an explosion has calculated the knockback velocity
     * but has not yet added the knockback to the entity caught in blast.
     *
     * @param level           The level that the explosion is in
     * @param explosion       Explosion that is happening
     * @param entity          The entity caught in the explosion's blast
     * @param initialVelocity The explosion calculated velocity for the entity
     * @return The new explosion velocity to add to the entity's existing velocity
     */
    public static Vec3 getExplosionKnockback(Level level, Explosion explosion, Entity entity, Vec3 initialVelocity) {
        ExplosionKnockbackEvent event = new ExplosionKnockbackEvent(level, explosion, entity, initialVelocity);
        if (EventGate.has(ExplosionKnockbackEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event.getKnockbackVelocity();
    }

    public static float onLivingHeal(LivingEntity entity, float amount) {
        LivingHealEvent event = new LivingHealEvent(entity, amount);
        return ((EventGate.has(LivingHealEvent.class) && Need.livingHeal(entity) && NeoForge.EVENT_BUS.post(event).isCanceled()) ? 0 : event.getAmount());
    }

    public static boolean onPotionAttemptBrew(NonNullList<ItemStack> stacks) {
        NonNullList<ItemStack> tmp = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
        for (int x = 0; x < tmp.size(); x++)
            tmp.set(x, stacks.get(x).copy());

        PotionBrewEvent.Pre event = new PotionBrewEvent.Pre(tmp);
        if ((EventGate.has(PotionBrewEvent.Pre.class) && NeoForge.EVENT_BUS.post(event).isCanceled())) {
            boolean changed = false;
            for (int x = 0; x < stacks.size(); x++) {
                changed |= ItemStack.matches(tmp.get(x), stacks.get(x));
                stacks.set(x, event.getItem(x));
            }
            if (changed)
                onPotionBrewed(stacks);
            return true;
        }
        return false;
    }

    public static void onPotionBrewed(NonNullList<ItemStack> brewingItemStacks) {
        if (EventGate.has(PotionBrewEvent.Post.class)) NeoForge.EVENT_BUS.post(new PotionBrewEvent.Post(brewingItemStacks));
    }

    /**
     * Checks if a sleeping entity can continue sleeping with the given sleeping problem.
     * 
     * @return true if the entity may continue sleeping
     */
    public static boolean canEntityContinueSleeping(LivingEntity sleeper, @Nullable BedSleepingProblem problem) {
        if (!EventGate.has(CanContinueSleepingEvent.class)) return problem == null;
        return NeoForge.EVENT_BUS.post(new CanContinueSleepingEvent(sleeper, problem)).mayContinueSleeping();
    }

    public static InteractionResultHolder<ItemStack> onArrowNock(ItemStack item, Level level, Player player, InteractionHand hand, boolean hasAmmo) {
        ArrowNockEvent event = new ArrowNockEvent(player, item, hand, level, hasAmmo);
        if ((EventGate.has(ArrowNockEvent.class) && NeoForge.EVENT_BUS.post(event).isCanceled()))
            return new InteractionResultHolder<ItemStack>(InteractionResult.FAIL, item);
        return event.getAction();
    }

    public static int onArrowLoose(ItemStack stack, Level level, Player player, int charge, boolean hasAmmo) {
        ArrowLooseEvent event = new ArrowLooseEvent(player, stack, level, charge, hasAmmo);
        if ((EventGate.has(ArrowLooseEvent.class) && NeoForge.EVENT_BUS.post(event).isCanceled()))
            return -1;
        return event.getCharge();
    }

    public static boolean onProjectileImpact(Projectile projectile, HitResult ray) {
        return EventGate.has(ProjectileImpactEvent.class) && NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(projectile, ray)).isCanceled();
    }

    /**
     * Use the {@link #loadLootTable(HolderLookup.Provider, ResourceLocation, LootTable) instead}
     */
    @Deprecated
    public static LootTable loadLootTable(ResourceLocation name, LootTable table) {
        return loadLootTable(RegistryAccess.EMPTY, name, table);
    }

    /**
     * Fires the {@link LootTableLoadEvent} for non-empty loot tables and returns the table if the event was not
     * canceled and the table was not set to {@link LootTable#EMPTY} in the event. Otherwise returns {@code null}
     * which maps to an empty {@link Optional} in {@link LootDataType#deserialize(ResourceLocation, DynamicOps, Object)}
     */
    @Nullable
    @ApiStatus.Internal
    public static LootTable loadLootTable(HolderLookup.Provider registries, ResourceLocation name, LootTable table) {
        if (table == LootTable.EMPTY) // Empty table has a null name, and shouldn't be modified anyway.
            return null;
        LootTableLoadEvent event = new LootTableLoadEvent(registries, name, table);
        if ((EventGate.has(LootTableLoadEvent.class) && NeoForge.EVENT_BUS.post(event).isCanceled()) || event.getTable() == LootTable.EMPTY)
            return null;
        return event.getTable();
    }

    public static boolean onEntityDestroyBlock(LivingEntity entity, BlockPos pos, BlockState state) {
        return !(EventGate.has(LivingDestroyBlockEvent.class) && NeoForge.EVENT_BUS.post(new LivingDestroyBlockEvent(entity, pos, state)).isCanceled());
    }

    /**
     * Checks if an entity can perform a griefing action.
     * <p>
     * If an entity is provided, this method fires {@link EntityMobGriefingEvent}.
     * If an entity is not provided, this method returns the value of {@link GameRules#RULE_MOBGRIEFING}.
     * 
     * @param level  The level of the action
     * @param entity The entity performing the action, or null if unknown.
     * @return
     */
    public static boolean canEntityGrief(Level level, @Nullable Entity entity) {
        if (entity == null)
            return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);

        return !EventGate.has(EntityMobGriefingEvent.class) ? level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) : NeoForge.EVENT_BUS.post(new EntityMobGriefingEvent(level, entity)).canGrief();
    }

    /**
     * Fires the {@link AlterGroundEvent} and retrieves the resulting {@link StateProvider}.
     * 
     * @param ctx       The tree decoration context for the current alteration.
     * @param positions The list of positions that are considered roots.
     * @param provider  The original {@link BlockStateProvider} from the {@link AlterGroundDecorator}.
     * @return The (possibly event-modified) {@link StateProvider} to be used for ground alteration.
     * @apiNote This method is called off-thread during world generation.
     */
    public static StateProvider alterGround(TreeDecorator.Context ctx, List<BlockPos> positions, StateProvider provider) {
        if (positions.isEmpty()) return provider; // I don't think this list is ever empty, but if it is, firing the event is pointless anyway.
        AlterGroundEvent event = new AlterGroundEvent(ctx, positions, provider);
        if (EventGate.has(AlterGroundEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event.getStateProvider();
    }

    public static void fireChunkWatch(ServerPlayer entity, LevelChunk chunk, ServerLevel level) {
        if (EventGate.has(ChunkWatchEvent.Watch.class)) NeoForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(entity, chunk, level));
    }

    public static void fireChunkSent(ServerPlayer entity, LevelChunk chunk, ServerLevel level) {
        if (EventGate.has(ChunkWatchEvent.Sent.class)) NeoForge.EVENT_BUS.post(new ChunkWatchEvent.Sent(entity, chunk, level));
    }

    public static void fireChunkUnWatch(ServerPlayer entity, ChunkPos chunkpos, ServerLevel level) {
        if (EventGate.has(ChunkWatchEvent.UnWatch.class)) NeoForge.EVENT_BUS.post(new ChunkWatchEvent.UnWatch(entity, chunkpos, level));
    }

    public static List<PreparableReloadListener> onResourceReload(ReloadableServerResources serverResources, RegistryAccess registryAccess) {
        AddReloadListenerEvent event = new AddReloadListenerEvent(serverResources, registryAccess);
        if (EventGate.has(AddReloadListenerEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event.getListeners();
    }

    public static void onCommandRegister(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection environment, CommandBuildContext context) {
        RegisterCommandsEvent event = new RegisterCommandsEvent(dispatcher, environment, context);
        if (EventGate.has(RegisterCommandsEvent.class)) NeoForge.EVENT_BUS.post(event);
    }

    public static EntityEvent.Size getEntitySizeForge(Entity entity, Pose pose, EntityDimensions size) {
        EntityEvent.Size evt = new EntityEvent.Size(entity, pose, size);
        if (EventGate.has(EntityEvent.Size.class)) NeoForge.EVENT_BUS.post(evt);
        return evt;
    }

    public static EntityEvent.Size getEntitySizeForge(Entity entity, Pose pose, EntityDimensions oldSize, EntityDimensions newSize) {
        EntityEvent.Size evt = new EntityEvent.Size(entity, pose, oldSize, newSize);
        if (EventGate.has(EntityEvent.Size.class)) NeoForge.EVENT_BUS.post(evt);
        return evt;
    }

    public static boolean canLivingConvert(LivingEntity entity, EntityType<? extends LivingEntity> outcome, Consumer<Integer> timer) {
        return !(EventGate.has(LivingConversionEvent.Pre.class) && NeoForge.EVENT_BUS.post(new LivingConversionEvent.Pre(entity, outcome, timer)).isCanceled());
    }

    public static void onLivingConvert(LivingEntity entity, LivingEntity outcome) {
        if (EventGate.has(LivingConversionEvent.Post.class)) NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post(entity, outcome));
    }

    public static EntityTeleportEvent.EnderEntity onEnderTeleport(LivingEntity entity, double targetX, double targetY, double targetZ) {
        EntityTeleportEvent.EnderEntity event = new EntityTeleportEvent.EnderEntity(entity, targetX, targetY, targetZ);
        if (EventGate.has(EntityTeleportEvent.EnderEntity.class)) NeoForge.EVENT_BUS.post(event);
        return event;
    }

    @ApiStatus.Internal
    public static EntityTeleportEvent.EnderPearl onEnderPearlLand(ServerPlayer entity, double targetX, double targetY, double targetZ, ThrownEnderpearl pearlEntity, float attackDamage, HitResult hitResult) {
        EntityTeleportEvent.EnderPearl event = new EntityTeleportEvent.EnderPearl(entity, targetX, targetY, targetZ, pearlEntity, attackDamage, hitResult);
        if (EventGate.has(EntityTeleportEvent.EnderPearl.class)) NeoForge.EVENT_BUS.post(event);
        return event;
    }

    public static EntityTeleportEvent.ChorusFruit onChorusFruitTeleport(LivingEntity entity, double targetX, double targetY, double targetZ) {
        EntityTeleportEvent.ChorusFruit event = new EntityTeleportEvent.ChorusFruit(entity, targetX, targetY, targetZ);
        if (EventGate.has(EntityTeleportEvent.ChorusFruit.class)) NeoForge.EVENT_BUS.post(event);
        return event;
    }

    public static boolean onPermissionChanged(GameProfile gameProfile, int newLevel, PlayerList playerList) {
        int oldLevel = playerList.getServer().getProfilePermissions(gameProfile);
        ServerPlayer player = playerList.getPlayer(gameProfile.getId());
        if (newLevel != oldLevel && player != null) {
            return EventGate.has(PermissionsChangedEvent.class) && NeoForge.EVENT_BUS.post(new PermissionsChangedEvent(player, newLevel, oldLevel)).isCanceled();
        }
        return false;
    }

    public static void firePlayerChangedDimensionEvent(Player player, ResourceKey<Level> fromDim, ResourceKey<Level> toDim) {
        if (EventGate.has(PlayerEvent.PlayerChangedDimensionEvent.class)) NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, fromDim, toDim));
    }

    public static void firePlayerLoggedIn(Player player) {
        if (EventGate.has(PlayerEvent.PlayerLoggedInEvent.class)) NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
    }

    /**
     * Called by {@link PlayerList#respawn(ServerPlayer, boolean)} after creating and initializing the new {@link ServerPlayer}.
     * 
     * @param player       The new player instance created by the respawn process
     * @param fromEndFight Whether the player is respawning because they jumped through the End return portal
     */
    public static void firePlayerRespawnEvent(ServerPlayer player, boolean fromEndFight) {
        if (EventGate.has(PlayerEvent.PlayerRespawnEvent.class)) NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, fromEndFight));
    }

    /**
     * Fires {@link EntityTickEvent.Pre}. Called from the head of {@link LivingEntity#tick()}.
     * 
     * @param entity The entity being ticked
     * @return Whether the event was cancelled
     */
    public static boolean isEntityTickPreCanceled(Entity entity) {
        return EventGate.has(EntityTickEvent.Pre.class) && NeoForge.EVENT_BUS.post(new EntityTickEvent.Pre(entity)).isCanceled();
    }

    /**
     * Fires {@link EntityTickEvent.Post}. Called from the tail of {@link LivingEntity#tick()}.
     * 
     * @param entity The entity being ticked
     */
    public static void fireEntityTickPost(Entity entity) {
        if (EventGate.has(EntityTickEvent.Post.class) && Need.entityTickPost(entity)) NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
    }

    /**
     * Fires {@link PlayerTickEvent.Post}. Called from the tail of {@link Player#tick()}.
     * 
     * @param player The player being ticked
     */
    public static void firePlayerTickPost(Player player) {
        if (EventGate.has(PlayerTickEvent.Post.class)) NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
    }

    /**
     * Fires {@link LevelTickEvent.Pre}. Called from {@link Minecraft#tick()} and {@link MinecraftServer#tickChildren(BooleanSupplier)} just before the try block for level tick is entered.
     * 
     * @param level    The level being ticked
     * @param haveTime The time supplier, indicating if there is remaining time to do work in the current tick.
     */
    public static void fireLevelTickPre(Level level, BooleanSupplier haveTime) {
        if (EventGate.has(LevelTickEvent.Pre.class)) NeoForge.EVENT_BUS.post(new LevelTickEvent.Pre(haveTime, level));
    }

    /**
     * Fires {@link LevelTickEvent.Post}. Called from {@link Minecraft#tick()} and {@link MinecraftServer#tickChildren(BooleanSupplier)} just after the try block for level tick is exited.
     * 
     * @param level    The level being ticked
     * @param haveTime The time supplier, indicating if there is remaining time to do work in the current tick.
     */
    public static void fireLevelTickPost(Level level, BooleanSupplier haveTime) {
        if (EventGate.has(LevelTickEvent.Post.class)) NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(haveTime, level));
    }

    /**
     * Fires {@link ServerTickEvent.Pre}. Called from the head of {@link MinecraftServer#tickServer(BooleanSupplier)}.
     * 
     * @param haveTime The time supplier, indicating if there is remaining time to do work in the current tick.
     * @param server   The current server
     */
    public static void fireServerTickPre(BooleanSupplier haveTime, MinecraftServer server) {
        if (EventGate.has(ServerTickEvent.Pre.class)) NeoForge.EVENT_BUS.post(new ServerTickEvent.Pre(haveTime, server));
    }

    /**
     * Fires {@link ServerTickEvent.Post}. Called from the tail of {@link MinecraftServer#tickServer(BooleanSupplier)}.
     * 
     * @param haveTime The time supplier, indicating if there is remaining time to do work in the current tick.
     * @param server   The current server
     */
    public static void fireServerTickPost(BooleanSupplier haveTime, MinecraftServer server) {
        if (EventGate.has(ServerTickEvent.Post.class)) NeoForge.EVENT_BUS.post(new ServerTickEvent.Post(haveTime, server));
    }

    private static final WeightedRandomList<MobSpawnSettings.SpawnerData> NO_SPAWNS = WeightedRandomList.create();

    public static WeightedRandomList<MobSpawnSettings.SpawnerData> getPotentialSpawns(LevelAccessor level, MobCategory category, BlockPos pos, WeightedRandomList<MobSpawnSettings.SpawnerData> oldList) {
        LevelEvent.PotentialSpawns event = new LevelEvent.PotentialSpawns(level, category, pos, oldList);
        if ((EventGate.has(LevelEvent.PotentialSpawns.class) && NeoForge.EVENT_BUS.post(event).isCanceled()))
            return NO_SPAWNS;
        else if (event.getSpawnerDataList() == oldList.unwrap())
            return oldList;
        return WeightedRandomList.create(event.getSpawnerDataList());
    }

    @ApiStatus.Internal
    public static void onAdvancementEarnedEvent(Player player, AdvancementHolder earned) {
        if (EventGate.has(AdvancementEarnEvent.class)) NeoForge.EVENT_BUS.post(new AdvancementEarnEvent(player, earned));
    }

    public static boolean onEffectRemoved(LivingEntity entity, Holder<MobEffect> effect, @Nullable EffectCure cure) {
        return EventGate.has(MobEffectEvent.Remove.class) && Need.effectRemove(effect) && NeoForge.EVENT_BUS.post(new MobEffectEvent.Remove(entity, effect, cure)).isCanceled();
    }

    public static boolean onEffectRemoved(LivingEntity entity, MobEffectInstance effectInstance, @Nullable EffectCure cure) {
        return EventGate.has(MobEffectEvent.Remove.class) && Need.effectRemove(effectInstance.getEffect()) && NeoForge.EVENT_BUS.post(new MobEffectEvent.Remove(entity, effectInstance, cure)).isCanceled();
    }

    /**
     * Fires {@link GetEnchantmentLevelEvent} and for a single enchantment, returning the (possibly event-modified) level.
     * 
     * @param level The original level of the enchantment as provided by the Item.
     * @param stack The stack being queried against.
     * @param ench  The enchantment being queried for.
     * @return The new level of the enchantment.
     */
    public static int getEnchantmentLevelSpecific(int level, ItemStack stack, Holder<Enchantment> ench) {
        RegistryLookup<Enchantment> lookup = ench.unwrapLookup();
        if (lookup == null) { // Pretty sure this is never null, but I can't *prove* that it isn't.
            return level;
        }

        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(ench, level);
        var event = new GetEnchantmentLevelEvent(stack, enchantments, ench, ench.unwrapLookup());
        NeoForge.EVENT_BUS.post(event);
        return enchantments.getLevel(ench);
    }

    /**
     * Fires {@link GetEnchantmentLevelEvent} and for all enchantments, returning the (possibly event-modified) enchantment map.
     * 
     * @param enchantments The original enchantment map as provided by the Item.
     * @param stack        The stack being queried against.
     * @return The new enchantment map.
     */
    public static ItemEnchantments getAllEnchantmentLevels(ItemEnchantments enchantments, ItemStack stack, RegistryLookup<Enchantment> lookup) {
        var mutableEnchantments = new ItemEnchantments.Mutable(enchantments);
        var event = new GetEnchantmentLevelEvent(stack, mutableEnchantments, null, lookup);
        NeoForge.EVENT_BUS.post(event);
        return mutableEnchantments.toImmutable();
    }

    /**
     * Fires the {@link BuildCreativeModeTabContentsEvent}.
     *
     * @param tab               The tab that contents are being collected for.
     * @param tabKey            The resource key of the tab.
     * @param originalGenerator The display items generator that populates vanilla entries.
     * @param params            Display parameters, controlling if certain items are hidden.
     * @param output            The output acceptor.
     * @apiNote Call via {@link CreativeModeTab#buildContents(CreativeModeTab.ItemDisplayParameters)}
     */
    @ApiStatus.Internal
    public static void onCreativeModeTabBuildContents(CreativeModeTab tab, ResourceKey<CreativeModeTab> tabKey, CreativeModeTab.DisplayItemsGenerator originalGenerator, CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
        final var parentEntries = new InsertableLinkedOpenCustomHashSet<ItemStack>(ItemStackLinkedSet.TYPE_AND_TAG);
        final var searchEntries = new InsertableLinkedOpenCustomHashSet<ItemStack>(ItemStackLinkedSet.TYPE_AND_TAG);

        originalGenerator.accept(params, (stack, vis) -> {
            if (stack.getCount() != 1)
                throw new IllegalArgumentException("The stack count must be 1");

            if (BuildCreativeModeTabContentsEvent.isParentTab(vis)) {
                parentEntries.add(stack);
            }

            if (BuildCreativeModeTabContentsEvent.isSearchTab(vis)) {
                searchEntries.add(stack);
            }
        });

        ModLoader.postEvent(new BuildCreativeModeTabContentsEvent(tab, tabKey, params, parentEntries, searchEntries));

        for (var entry : parentEntries) {
            output.accept(entry, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }

        for (var entry : searchEntries) {
            output.accept(entry, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }
    }

    /**
     * Fires the mob split event. Returns the event for cancellation checking.
     * 
     * @param parent   The parent mob, which is in the process of being removed.
     * @param children All child mobs that would have normally spawned.
     * @return The event object.
     */
    public static MobSplitEvent onMobSplit(Mob parent, List<Mob> children) {
        var event = new MobSplitEvent(parent, children);
        if (EventGate.has(MobSplitEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event;
    }

    /**
     * Fires the {@link ModifyCustomSpawnersEvent}. Returns the custom spawners list.
     * 
     * @param serverLevel    The server level.
     * @param customSpawners The original custom spawners.
     * @return The new custom spawners list.
     */
    public static List<CustomSpawner> getCustomSpawners(ServerLevel serverLevel, List<CustomSpawner> customSpawners) {
        ModifyCustomSpawnersEvent event = new ModifyCustomSpawnersEvent(serverLevel, new ArrayList<>(customSpawners));
        if (EventGate.has(ModifyCustomSpawnersEvent.class)) NeoForge.EVENT_BUS.post(event);
        return event.getCustomSpawners();
    }
}
