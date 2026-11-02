// TRACKED HASH: 8ce7cfcc1608a79d687631411c28c60d1064aad3
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.level;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FriendlyByteBufUtil;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload;
import net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.injections.server.level.ServerPlayerInjection;
import dev.nytheria.compat.injections.world.entity.player.PlayerInjection;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;

@Mixin(value = ServerPlayer.class, priority = 1100)
public abstract class ServerPlayerInject extends Player implements ServerPlayerInjection {
    // Nytheria Compat: this is the anonymous ContainerSynchronizer (ServerPlayer$1, intermediary class_3222$1) - $2 is the ContainerListener.
    // It used to target "ServerPlayer$0", which doesn't exist, so the mixin never applied and container data
    // values were always truncated to a short by the vanilla ClientboundContainerSetDataPacket.
    @Mixin(targets = "net.minecraft.server.level.ServerPlayer$1")
    public abstract static class AnonymousContainerSynchronizer1Inject {
        // Nytheria Compat: private method_34227, used by both sendInitialData and sendDataChange. Its body is a single
        //  "this$0.connection.send(new ClientboundContainerSetDataPacket(container.containerId, id, value))", so this wraps that one
        //  send (there is no other send call to pick an ordinal for). It used to be a HEAD inject that cancelled the method for every client with the
        //  channel, so other mods' injections into broadcastDataValue never ran for Nytheria Compat clients; now the vanilla body always runs and only
        //  the packet it sends is replaced, with the same condition and payload as before.
        @WrapOperation(method = "broadcastDataValue", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
        private void nytheriacompat$tryUseAdvancedContainerSetDataPayload(ServerGamePacketListenerImpl instance, Packet<?> packet, Operation<Void> original, @Local(argsOnly = true) AbstractContainerMenu container, @Local(argsOnly = true, ordinal = 0) int id, @Local(argsOnly = true, ordinal = 1) int value) {
            if (instance.hasChannel(AdvancedContainerSetDataPayload.TYPE)) {
                instance.send(new AdvancedContainerSetDataPayload((byte) container.containerId, (short) id, value));
            } else {
                original.call(instance, packet);
            }
        }
    }

    @Shadow public ServerGamePacketListenerImpl connection;
    @Shadow @Final public ServerPlayerGameMode gameMode;
    @Shadow private @Nullable Entity camera;

    public ServerPlayerInject(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Inject(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;gameEvent(Lnet/minecraft/core/Holder;)V", shift = At.Shift.AFTER), cancellable = true)
    private void nytheriacompat$callForgeLivingDeath(DamageSource damageSource, CallbackInfo ci) {
        if (this.nytheriacompat$postVanillaLivingDeath(damageSource))
            ci.cancel();
    }

    @Inject(method = "changeDimension", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerPlayer;lastSentFood:I", shift = At.Shift.AFTER))
    private void nytheriacompat$firePlayerChangedDimensionEventVanilla(DimensionTransition transition, CallbackInfoReturnable<Entity> cir, @Local ResourceKey<Level> oldLevel) {
        // Nytheria Compat: on the client all attachments are lost on dimension change and need to be re-sent (like NeoForge)
        AttachmentSync.syncInitialPlayerAttachments((ServerPlayer) (Object) this);
        EventHooks.firePlayerChangedDimensionEvent((ServerPlayer) (Object) this, oldLevel, transition.newLevel().dimension());
    }

    // Handled by Fabric API
    /*@Inject(at = @At("HEAD"), method = "startSleepInBed", cancellable = true)
    public void nytheriacompat$checkPlayerSleepEvent(BlockPos blockPos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        var ret = ForgeEventFactory.onPlayerSleepInBed((ServerPlayer) (Object) this, Optional.of(blockPos));
        if (ret != null)
            cir.setReturnValue(Either.left(ret));
    }*/

    @Unique private Consumer<RegistryFriendlyByteBuf> nytheriacompat$extraDataWriter = null;

    @Override
    public OptionalInt openMenu(MenuProvider menuProvider, Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
        this.nytheriacompat$extraDataWriter = extraDataWriter;
        OptionalInt result = this.openMenu(menuProvider);
        this.nytheriacompat$extraDataWriter = null;

        return result;
    }

    @WrapOperation(method = "openMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;closeContainer()V"))
    private void nytheriacompat$checkShouldTriggerClientSideClosing(ServerPlayer instance, Operation<Void> original, @Local(argsOnly = true) MenuProvider provider) {
        if (provider.shouldTriggerClientSideContainerClosingOnOpen()) {
            original.call(instance);
        } else {
            instance.doCloseContainer();
        }
    }

    @WrapOperation(method = "openMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void nytheriacompat$sendAdditionalArbitraryData(ServerGamePacketListenerImpl instance, Packet<?> packet, Operation<Void> original, @Local AbstractContainerMenu menu, @Local(argsOnly = true) MenuProvider menuProvider) {
        Consumer<RegistryFriendlyByteBuf> extraDataWriter = this.nytheriacompat$extraDataWriter;

        var extraData = FriendlyByteBufUtil.writeCustomData(buffer -> {
            this.lambda$openMenu$15(menuProvider, menu, extraDataWriter, buffer);
        }, this.registryAccess());

        if (extraData.length != 0) {
            instance.send(new AdvancedOpenScreenPayload(menu.containerId, menu.getType(), menuProvider.getDisplayName(), extraData));
        } else {
            original.call(instance, packet);
        }
    }

    // Nytheria Compat: funny workaround
    @Unique
    private void lambda$openMenu$15(MenuProvider provider, AbstractContainerMenu menu, Consumer<RegistryFriendlyByteBuf> extraDataWriter, RegistryFriendlyByteBuf buffer) {
        try {
            provider.writeClientSideData(menu, buffer);
            if (extraDataWriter != null) {
                extraDataWriter.accept(buffer);
            }
        } catch (Throwable e) {
            // Nytheria Compat: knowing me, I'm gonna get confused, and other people will get confused. Lemme just make that a little easier on everyone.
            throw new RuntimeException("An exception occurred in lambda$openMenu$15, which is a Nytheria Compat addition!!!", e);
        }
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void nytheriacompat$storeTabListData(ServerPlayer that, boolean keepEverything, CallbackInfo ci) {
        CompoundTag old = that.getPersistentData();
        if (old.contains(PlayerInjection.PERSISTED_NBT_TAG)) {
            this.getPersistentData().put(PlayerInjection.PERSISTED_NBT_TAG, old.get(PlayerInjection.PERSISTED_NBT_TAG));
        }

        EventHooks.onPlayerClone(this, that, !keepEverything);
        this.tabListHeader = that.getTabListHeader();
        this.tabListFooter = that.getTabListFooter();
    }

    @Inject(method = "setCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerPlayer;camera:Lnet/minecraft/world/entity/Entity;", ordinal = 0))
    private void nytheriacompat$usePartEntityParentCamera(Entity entityToSpectate, CallbackInfo ci) {
        while (this.camera instanceof PartEntity<?> partEntity)
            this.camera = partEntity.getParent();
    }

    // Handled by Fabric API
    /*@Inject(method = "setRespawnPosition", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$checkPlayerSpawnSetEvent(ResourceKey<Level> dimension, @Nullable BlockPos position, float angle, boolean forced, boolean sendMessage, CallbackInfo ci) {
        if (EventHooks.onPlayerSpawnSet(this, dimension == null ? Level.OVERWORLD : dimension, position, forced))
            ci.cancel();
    }*/

    @Shadow private String language;

    @Override
    public String getLanguage() {
        return language;
    }

    @Unique private Component tabListHeader = Component.empty();
    @Unique private Component tabListFooter = Component.empty();

    @Override
    public Component getTabListHeader() {
        return tabListHeader;
    }

    @Override
    public Component getTabListFooter() {
        return tabListFooter;
    }

    @Override
    public void setTabListHeader(Component tabListHeader) {
        setTabListHeaderFooter(tabListHeader, this.tabListFooter);
    }

    @Override
    public void setTabListFooter(Component tabListFooter) {
        this.setTabListHeaderFooter(this.tabListHeader, tabListFooter);
    }

    @Override
    public void setTabListHeaderFooter(Component header, Component footer) {
        if (Objects.equals(header, this.tabListHeader) && Objects.equals(footer, this.tabListFooter))
            return;

        this.tabListHeader = Objects.requireNonNull(header, "header");
        this.tabListFooter = Objects.requireNonNull(footer, "footer");

        this.connection.send(new ClientboundTabListPacket(header, footer));
    }

    @WrapOperation(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean nytheriacompat$captureDrops(Level instance, Entity entity, Operation<Boolean> original) {
        if (this.captureDrops() != null) {
            this.captureDrops().add((ItemEntity) entity);
            return false;
        }

        return original.call(instance, entity);
    }

    @WrapOperation(method = "drop(Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity nytheriacompat$callPlayerTossEvent(ServerPlayer instance, ItemStack droppedItem, boolean dropAround, boolean includeThrowerName, Operation<ItemEntity> original) {
        return CommonHooks.nytheriacompat$onPlayerTossEvent(instance, droppedItem, () -> original.call(instance, droppedItem, dropAround, includeThrowerName));
    }
}
