// TRACKED HASH: 1886f30859644767992d36dcae1264c9b9614cd4
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.injections.client.multiplayer.CommonListenerCookieInjection;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplInject extends ServerCommonPacketListenerImpl {
    @Shadow
    public ServerPlayer player;

    public ServerGamePacketListenerImplInject(MinecraftServer server, Connection connection, CommonListenerCookie cookie) {
        super(server, connection, cookie);
    }

    @WrapOperation(method = "handleMoveVehicle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;absMoveTo(DDDFF)V"))
    private void nytheriacompat$resyncPlayerWithVehicleOnMove(Entity instance, double x, double y, double z, float yRot, float xRot, Operation<Void> original) {
        original.call(instance, x, y, z, yRot, xRot);
        this.resyncPlayerWithVehicle(instance);
    }

    @Unique
    private void resyncPlayerWithVehicle(Entity vehicle) {
        Vec3 oldPos = this.player.position();
        float yRot = this.player.getYRot();
        float xRot = this.player.getXRot();
        float yHeadRot = this.player.getYHeadRot();

        vehicle.positionRider(this.player);

        // Preserve old rotation and store old position in old x/y/z
        this.player.setYRot(yRot);
        this.player.setXRot(xRot);
        this.player.setYHeadRot(yHeadRot);
        this.player.xo = oldPos.x;
        this.player.yo = oldPos.y;
        this.player.zo = oldPos.z;
    }

    // Nytheria Compat: why does Neo change it to not use the ackBlockChangesUpTo method????

    // Nytheria Compat: NeoForge's EntityInteractSpecific (ServerGamePacketListenerImpl$1#onInteraction) is bridged from Porting Lib
    //  (NytheriaCompat.kt), whose mixin fires at the same point. Firing it here as well fired it twice.

    @ModifyExpressionValue(method = "handleConfigurationAcknowledged", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;createCookie(Lnet/minecraft/server/level/ClientInformation;)Lnet/minecraft/server/network/CommonListenerCookie;"))
    private CommonListenerCookie nytheriacompat$appendConnectionTypeToCookie(CommonListenerCookie original) {
        ((CommonListenerCookieInjection) (Object) original).nytheriacompat$setConnectionType(this.getConnectionType());
        return original;
    }

}