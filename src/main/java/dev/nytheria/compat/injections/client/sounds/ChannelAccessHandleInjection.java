/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.sounds;

import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;

import java.util.UUID;

public interface ChannelAccessHandleInjection {
    void nytheriacompat$setPool(Library.Pool pool);
    void nytheriacompat$setSoundInstance(SoundInstance instance);
    void nytheriacompat$setSoundEngine(SoundEngine engine);
}
