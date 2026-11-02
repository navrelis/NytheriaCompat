// TRACKED HASH: 8fda2624182ac03df35817374f8cf966a4ed0fb0
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.multiplayer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.brigadier.CommandDispatcher;
import net.neoforged.neoforge.client.ClientCommandHandler;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.world.item.alchemy.PotionBrewingInjection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerInject extends ClientCommonPacketListenerImpl {
    @Shadow
    @Final
    private RegistryAccess.Frozen registryAccess;

    @Shadow
    protected abstract void startWaitingForNewLevel(LocalPlayer player, ClientLevel level, ReceivingLevelScreen.Reason reason);

    @Shadow
    private CommandDispatcher<SharedSuggestionProvider> commands;
    private ConnectionType connectionType;

    protected ClientPacketListenerInject(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/alchemy/PotionBrewing;bootstrap(Lnet/minecraft/world/flag/FeatureFlagSet;)Lnet/minecraft/world/item/alchemy/PotionBrewing;"))
    private void nytheriacompat$storeConnectionType(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
        this.connectionType = commonListenerCookie.connectionType();
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/alchemy/PotionBrewing;bootstrap(Lnet/minecraft/world/flag/FeatureFlagSet;)Lnet/minecraft/world/item/alchemy/PotionBrewing;"))
    private PotionBrewing nytheriacompat$trySetCurrentRegistryAccess(FeatureFlagSet enabledFeatures, Operation<PotionBrewing> original) {
        PotionBrewingInjection.nytheriacompat$registryAccess.set(this.registryAccess);
        var result = original.call(enabledFeatures);
        PotionBrewingInjection.nytheriacompat$registryAccess.set(RegistryAccess.EMPTY); // just in case
        return result;
    }

    @Inject(method = "handleLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetPos()V", shift = At.Shift.AFTER))
    public void nytheriacompat$fireForgeLoginEvent(ClientboundLoginPacket clientboundLoginPacket, CallbackInfo ci) {
        ClientHooks.firePlayerLogin(this.minecraft.gameMode, this.minecraft.player, this.minecraft.getConnection().getConnection());
    }

    @Unique private ResourceKey<Level> nytheriacompat$fromDimension;
    @Unique private ResourceKey<Level> nytheriacompat$toDimension;

    @Unique
    private void startWaitingForNewLevel(LocalPlayer player, ClientLevel level, ReceivingLevelScreen.Reason reason, @Nullable ResourceKey<Level> toDimension, @Nullable ResourceKey<Level> fromDimension) {
        this.nytheriacompat$fromDimension = fromDimension;
        this.nytheriacompat$toDimension = toDimension;

        this.startWaitingForNewLevel(player, level, reason);

        this.nytheriacompat$fromDimension = null;
        this.nytheriacompat$toDimension = null;
    }

    @Inject(method = "handleCommands", at = @At("TAIL"))
    private void nytheriacompat$mergeCommands(ClientboundCommandsPacket packet, CallbackInfo ci, @Share("context") LocalRef<CommandBuildContext> context) {
        this.commands = ClientCommandHandler.mergeServerCommands(this.commands, context.get());
    }

    @Override
    public ConnectionType getConnectionType() {
        return connectionType;
    }
}
