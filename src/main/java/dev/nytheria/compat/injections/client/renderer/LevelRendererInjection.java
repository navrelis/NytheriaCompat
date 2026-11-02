/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.function.Consumer;

public interface LevelRendererInjection {
    default Frustum getFrustum() {
        throw NytheriaCompatHelper.createMixinException(LevelRendererInjection.class, "getFrustum");
    }

    default int getTicks() {
        throw NytheriaCompatHelper.createMixinException(LevelRendererInjection.class, "getTicks");
    }

    default void iterateVisibleBlockEntities(Consumer<BlockEntity> blockEntityConsumer) {
        throw NytheriaCompatHelper.createMixinException(LevelRendererInjection.class, "blockEntityConsumer");
    }

    default void requestOutlineEffect() {
        throw NytheriaCompatHelper.createMixinException(LevelRendererInjection.class, "requestOutlineEffect");
    }
}
