/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.texture;

import io.github.fabricators_of_create.porting_lib.extensions.client.AbstractTextureExtension;
import dev.nytheria.compat.util.NytheriaCompatHelper;

public interface AbstractTextureInjection extends AbstractTextureExtension {
    default void setBlurMipmap(boolean blur, boolean mipmap) {
        throw NytheriaCompatHelper.createMixinException(AbstractTextureInjection.class, "setBlurMipmap");
    }

    default void restoreLastBlurMipmap() {
        throw NytheriaCompatHelper.createMixinException(AbstractTextureInjection.class, "restoreLastBlurMipmap");
    }
}
