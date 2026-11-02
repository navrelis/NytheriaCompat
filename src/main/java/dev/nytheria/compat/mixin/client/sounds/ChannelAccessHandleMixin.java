/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.client.sounds;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import dev.nytheria.compat.hook.EventGate;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.SoundConsumerStorage;
import dev.nytheria.compat.injections.client.sounds.ChannelAccessHandleInjection;

import java.util.function.Consumer;

@Mixin(ChannelAccess.ChannelHandle.class)
public abstract class ChannelAccessHandleMixin implements ChannelAccessHandleInjection {
    @Shadow @Nullable private Channel channel;
    @Unique private Library.Pool nytheriacompat$pool;
    @Unique private SoundEngine nytheriacompat$soundEngine;
    @Unique private SoundInstance nytheriacompat$soundInstance;

    @Override
    public void nytheriacompat$setPool(Library.Pool pool) {
        this.nytheriacompat$pool = pool;
    }

    @Override
    public void nytheriacompat$setSoundEngine(SoundEngine engine) {
        this.nytheriacompat$soundEngine = engine;
    }

    @Override
    public void nytheriacompat$setSoundInstance(SoundInstance instance) {
        this.nytheriacompat$soundInstance = instance;
    }

    @Inject(method = "method_19737", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private void nytheriacompat$callPlaySoundEvents(Consumer<Channel> consumer, CallbackInfo ci) {
        if (this.channel != null && nytheriacompat$soundEngine != null && nytheriacompat$soundInstance != null && SoundConsumerStorage.soundConsumerChannels.remove(consumer)) {
            if (nytheriacompat$pool == Library.Pool.STATIC) {
                if (EventGate.has(PlaySoundSourceEvent.class)) NeoForge.EVENT_BUS.post(new PlaySoundSourceEvent(nytheriacompat$soundEngine, nytheriacompat$soundInstance, this.channel));
            } else if (nytheriacompat$pool == Library.Pool.STREAMING) {
                if (EventGate.has(PlayStreamingSourceEvent.class)) NeoForge.EVENT_BUS.post(new PlayStreamingSourceEvent(nytheriacompat$soundEngine, nytheriacompat$soundInstance, this.channel));
            }
        }
    }
}
