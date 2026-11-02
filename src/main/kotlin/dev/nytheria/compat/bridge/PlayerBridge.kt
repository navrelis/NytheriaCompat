/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.bridge

import com.mojang.datafixers.util.Either
import io.github.fabricators_of_create.porting_lib.entity.events.player.CriticalHitEvent
import io.github.fabricators_of_create.porting_lib.entity.events.player.PlayerInteractEvent
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Unit
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.CommonHooks
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.common.extensions.IBlockExtension
import net.neoforged.neoforge.event.EventHooks
import dev.nytheria.compat.hook.EventGate
import dev.nytheria.compat.util.NytheriaCompatHelper

/**
 * Fabric and Porting Lib player events (interaction, critical hits, sleeping, elytra) forwarded to NeoForge's event bus
 * and extension methods.
 */
object PlayerBridge {
    @Suppress("removal")
    fun register() {
        // Nytheria Compat: CommonHooks returns the cancellation result only when the NeoForge event was cancelled. Porting Lib only
        //  uses the result of a cancelled event, so the event has to be cancelled as well (null would also wipe the
        //  result of a Porting Lib listener).
        PlayerInteractEvent.RightClickItem.EVENT.register { event ->
            // Nytheria Compat: on the client MultiPlayerGameModeInject already fires NeoForge's RightClickItem at the same point.
            if (event.entity.level().isClientSide)
                return@register

            val cancelResult = CommonHooks.onItemRightClick(event.entity, event.hand)
            if (cancelResult != null) {
                event.cancellationResult = cancelResult
                event.isCanceled = true
            }
        }

        PlayerInteractEvent.EntityInteract.EVENT.register { event ->
            val cancelResult = CommonHooks.onInteractEntity(event.entity, event.target, event.hand)
            if (cancelResult != null) {
                event.cancellationResult = cancelResult
                event.isCanceled = true
            }
        }

        // Nytheria Compat: the only source of NeoForge's EntityInteractSpecific on both sides; Porting Lib fires it at the same
        //  points as NeoForge (before Entity.interactAt).
        PlayerInteractEvent.EntityInteractSpecific.EVENT.register { event ->
            val cancelResult = CommonHooks.onInteractEntityAt(event.entity, event.target, event.localPos, event.hand)
            if (cancelResult != null) {
                event.cancellationResult = cancelResult
                event.isCanceled = true
            }
        }

        PlayerInteractEvent.LeftClickBlock.EVENT.register { event ->
            // Nytheria Compat: on the client MultiPlayerGameModeInject already fires NeoForge's LeftClickBlock (and CLIENT_HOLD
            //  through onClientMineHold) at the same points, only the server side comes from Porting Lib.
            if (event.entity.level().isClientSide)
                return@register

            val forgeEvent = CommonHooks.onLeftClickBlock(event.entity, event.pos, event.face, when (event.action) {
                PlayerInteractEvent.LeftClickBlock.Action.START -> ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                PlayerInteractEvent.LeftClickBlock.Action.STOP -> ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
                PlayerInteractEvent.LeftClickBlock.Action.ABORT -> ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK
                else -> ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
            })

            // Nytheria Compat: Porting Lib reads useBlock (BlockState.attack) from its own event
            if (!forgeEvent.useBlock.isDefault)
                event.useBlock = TriState.of(forgeEvent.useBlock.isTrue)

            if (!forgeEvent.useItem.isDefault)
                event.useItem = TriState.of(forgeEvent.useItem.isTrue)

            if (forgeEvent.isCanceled)
                event.isCanceled = true
        }

        // Nytheria Compat: NeoForge fires CriticalHitEvent once in Player.attack, with the attacked entity as target, and uses
        //  isCriticalHit and the damage multiplier of the result. Porting Lib fires its CriticalHitEvent at that point.
        CriticalHitEvent.EVENT.register { event ->
            if (!EventGate.has(net.neoforged.neoforge.event.entity.player.CriticalHitEvent::class.java)) return@register

            val forgeEvent = net.neoforged.neoforge.event.entity.player.CriticalHitEvent(event.entity, event.target, event.vanillaMultiplier, event.isVanillaCritical)
            forgeEvent.isCriticalHit = event.isCriticalHit
            forgeEvent.damageMultiplier = event.damageMultiplier
            NeoForge.EVENT_BUS.post(forgeEvent)

            event.isCriticalHit = forgeEvent.isCriticalHit
            event.damageMultiplier = forgeEvent.damageMultiplier
        }

        PlayerInteractEvent.LeftClickEmpty.EVENT.register { event ->
            CommonHooks.onEmptyLeftClick(event.entity)
        }

        // Nytheria Compat: NeoForge's RightClickEmpty is fired by MinecraftInject#rightClickAir, the same point where Porting Lib
        //  fires its RightClickEmpty. Bridging it as well fired it twice per hand.

        EntitySleepEvents.ALLOW_SLEEPING.register { player, pos ->
            if (player !is ServerPlayer) // istg
                return@register null

            EventHooks.canPlayerStartSleeping(player, pos, Either.right(Unit.INSTANCE)).left().orElse(null)
        }

        EntitySleepEvents.SET_BED_OCCUPATION_STATE.register { entity, pos, state, occupied ->
            if (
                NytheriaCompatHelper.hasMethodOverride(
                    state.block.javaClass, IBlockExtension::class.java, "setBedOccupied",
                    BlockState::class.java, Level::class.java, BlockPos::class.java,
                    LivingEntity::class.java, Boolean::class.javaPrimitiveType!!
                )
            ) {
                state.setBedOccupied(entity.level(), pos, entity, occupied)
                return@register true
            } else {
                return@register false
            }
        }

        EntitySleepEvents.ALLOW_BED.register { entity, pos, state, bool ->
            if (
                NytheriaCompatHelper.hasMethodOverride(
                    state.block.javaClass, IBlockExtension::class.java, "isBed",
                    BlockState::class.java, BlockGetter::class.java, BlockPos::class.java, LivingEntity::class.java
                )
            ) {
                return@register if (state.isBed(entity.level(), pos, entity)) {
                    InteractionResult.SUCCESS
                } else {
                    InteractionResult.FAIL
                }
            }
            return@register InteractionResult.PASS
        }

        EntitySleepEvents.MODIFY_SLEEPING_DIRECTION.register { entity, pos, direction ->
            val state: BlockState = entity.level().getBlockState(pos)
            if (
                (
                    NytheriaCompatHelper.hasMethodOverride(
                        state.block.javaClass, IBlockExtension::class.java, "getBedDirection",
                        BlockState::class.java, LevelReader::class.java, BlockPos::class.java
                    ) || // If bed is not BedBlock we need to run the NeoForge getBedDirection for the correct result even if not overridden.
                    NytheriaCompatHelper.hasMethodOverride(
                        state.block.javaClass, IBlockExtension::class.java, "isBed",
                        BlockState::class.java, BlockGetter::class.java, BlockPos::class.java, LivingEntity::class.java
                    )
                ) &&
                state.isBed(entity.level(), pos, entity)
            ) {
                return@register state.getBedDirection(entity.level(), pos)
            }
            return@register direction
        }

        EntitySleepEvents.ALLOW_SETTING_SPAWN.register { player, pos ->
            !EventHooks.onPlayerSpawnSet(player, player.level().dimension(), pos, false)
        }

        EntityElytraEvents.CUSTOM.register { entity, tickElytra ->
            val chestPiece = entity.getItemBySlot(EquipmentSlot.CHEST)
            chestPiece.canElytraFly(entity) && chestPiece.elytraFlightTick(entity, entity.fallFlyingTicks)
        }
    }
}
