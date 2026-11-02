// TRACKED HASH: d6c99e89bffae0bd644b2ba83c1c0a920ac2fb71
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.texture;

import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.injections.client.renderer.texture.AbstractTextureInjection;

@Mixin(AbstractTexture.class)
public abstract class AbstractTextureInject implements AbstractTextureInjection {
    // implemented by Porting Lib

    @Override
    public void setBlurMipmap(boolean blur, boolean mipmap) {
        this.port_lib$setBlurMipmap(blur, mipmap);
    }

    @Override
    public void restoreLastBlurMipmap() {
        this.port_lib$restoreLastBlurMipmap();
    }

    /*@Shadow protected boolean blur;
    @Shadow protected boolean mipmap;

    @Shadow public abstract void setFilter(boolean blur, boolean mipmap);

    @Unique private boolean lastBlur;
    @Unique private boolean lastMipmap;

    public void setBlurMipmap(boolean blur, boolean mipmap) {
        this.lastBlur = this.blur;
        this.lastMipmap = this.mipmap;
        setFilter(blur, mipmap);
    }

    public void restoreLastBlurMipmap() {
        setFilter(this.lastBlur, this.lastMipmap);
    }*/
}