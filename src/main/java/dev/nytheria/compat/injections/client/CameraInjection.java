/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client;

import net.minecraft.client.Camera;
import net.minecraft.world.level.block.state.BlockState;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@FabricInjectedInterface(Camera.class)
public interface CameraInjection {
    default void setRotation(float yaw, float pitch, float roll) {
        throw NytheriaCompatHelper.createMixinException(CameraInjection.class, "setRotation");
    }

    default float getRoll() {
        throw NytheriaCompatHelper.createMixinException(CameraInjection.class, "getRoll");
    }

    default BlockState getBlockAtCamera() {
        throw NytheriaCompatHelper.createMixinException(CameraInjection.class, "getBlockAtCamera");
    }
}
