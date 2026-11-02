// TRACKED HASH: 988ae85739fcd12e564f775b34a9e6306e31f01f
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ViewportEvent;
import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import dev.nytheria.compat.injections.client.CameraInjection;
import dev.nytheria.compat.util.CameraRoll;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@Mixin(Camera.class)
public abstract class CameraInject implements CameraInjection {
    @Shadow private float yRot;
    @Shadow private float xRot;
    @Shadow private BlockGetter level;
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Unique private float nytheriacompat$roll;

    // Nytheria Compat: runs in every frame, resolve the override check only once.
    @Unique private static final NytheriaCompatHelper.MethodOverrideCheck nytheriacompat$SET_ROTATION_OVERRIDE = NytheriaCompatHelper.MethodOverrideCheck.of(Camera.class, "setRotation", float.class, float.class, float.class);

    @WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0))
    private void nytheriacompat$handleCameraAngleSetup(Camera instance, float yRot, float xRot, Operation<Void> original, @Local(argsOnly = true) float partialTick) {
        var yaw = yRot;
        var pitch = xRot;
        var roll = 0f;
        if (EventGate.has(ViewportEvent.ComputeCameraAngles.class)) {
            var cameraSetup = NeoForge.EVENT_BUS.post(new ViewportEvent.ComputeCameraAngles(instance, partialTick, yRot, xRot, 0));
            yaw = cameraSetup.getYaw();
            pitch = cameraSetup.getPitch();
            roll = cameraSetup.getRoll();
        }

        if (nytheriacompat$SET_ROTATION_OVERRIDE.test(this.getClass())) {
            instance.setRotation(yaw, pitch, roll);
        } else {
            this.nytheriacompat$roll = roll;
            original.call(instance, yaw, pitch);
        }
    }

    @WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1))
    private void nytheriacompat$handleThirdPersonReverse(Camera instance, float yRot, float xRot, Operation<Void> original, @Local(argsOnly = true) float partialTick) {
        if (nytheriacompat$SET_ROTATION_OVERRIDE.test(this.getClass())) {
            this.setRotation(yRot, xRot, -this.nytheriacompat$roll);
        } else {
            this.nytheriacompat$roll = -this.nytheriacompat$roll;
            original.call(instance, yRot, xRot);
        }
    }

    // Nytheria Compat: NeoForge's two-argument setRotation hands on a roll of 0, so the sleeping view does not keep the roll of the event.
    @WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 2))
    private void nytheriacompat$handleSleepingView(Camera instance, float yRot, float xRot, Operation<Void> original) {
        if (nytheriacompat$SET_ROTATION_OVERRIDE.test(this.getClass())) {
            this.setRotation(yRot, xRot, 0.0F);
        } else {
            this.nytheriacompat$roll = 0.0F;
            original.call(instance, yRot, xRot);
        }
    }

    @Definition(id = "getMaxZoom", method = "Lnet/minecraft/client/Camera;getMaxZoom(F)F")
    @Expression("-this.getMaxZoom(@(4.0) * ?)")
    @ModifyExpressionValue(method = "setup", at = @At("MIXINEXTRAS:EXPRESSION"))
    private float nytheriacompat$tryGetDetachedCameraDistance(float original, @Local(argsOnly = true, ordinal = 1) boolean thirdPersonReverse, @Local(ordinal = 1) float scale) {
        return ClientHooks.getDetachedCameraDistance((Camera) (Object) this, thirdPersonReverse, scale, original);
    }

    @Override
    public void setRotation(float yaw, float pitch, float roll) {
        this.nytheriacompat$roll = roll;
        this.setRotation(yaw, pitch);
    }

    // Nytheria Compat: the roll is in degrees, the argument is in radians, and NeoForge passes -roll * (PI / 180) (see CameraRoll).
    @ModifyArg(method = "setRotation", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"), index = 2)
    private float nytheriacompat$addRollToRotation(float angleZ) {
        return CameraRoll.addRoll(angleZ, this.nytheriacompat$roll);
    }

    @Override
    public float getRoll() {
        return this.nytheriacompat$roll;
    }

}