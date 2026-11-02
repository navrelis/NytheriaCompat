// TRACKED HASH: c90ad9c5c8bd04fe0240bdfe4249e3f318e2cd46
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.effect;

import com.google.common.collect.Sets;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.effect.MobEffectInstanceInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.Optional;
import java.util.Set;

@Implements(@Interface(iface = MobEffectInstanceInjection.class, prefix = "nytheriacompat$i$"))
@Mixin(value = MobEffectInstance.class, priority = 1010)
public abstract class MobEffectInstanceInject implements MobEffectInstanceInjection {
    @Shadow @Final private Holder<MobEffect> effect;

    @Inject(method = "<init>(Lnet/minecraft/core/Holder;IIZZZLnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("TAIL"))
    private void nytheriacompat$storeEffectCures(Holder effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon, MobEffectInstance hiddenEffect, CallbackInfo ci) {
        this.effect.value().neo$fillEffectCures(this.cures, (MobEffectInstance) (Object) this);
    }

    @Inject(method = "setDetailsFrom", at = @At("TAIL"))
    private void nytheriacompat$copyCuresFromOther(MobEffectInstance effectInstance, CallbackInfo ci) {
        this.cures.clear();
        this.cures.addAll(effectInstance.neoforge$getCures());
    }

    // TODO: impl sort order

    @Unique private final Set<EffectCure> cures = Sets.newIdentityHashSet();

    @Override
    public Set<EffectCure> neoforge$getCures() {
        return cures;
    }

    @Mixin(MobEffectInstance.Details.class)
    public abstract static class DetailsInject implements MobEffectInstanceInjection.DetailsInjection {
        @Unique private Optional<Set<EffectCure>> cures = Optional.empty();

        @Override
        public Optional<Set<EffectCure>> cures() {
            return this.cures;
        }

        @Override
        public void nytheriacompat$setCures(Optional<Set<EffectCure>> cures) {
            this.cures = cures;
        }

        public DetailsInject(int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon, Optional<MobEffectInstance.Details> hiddenEffect) {}

        @CreateInitializer
        public DetailsInject(int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon, Optional<MobEffectInstance.Details> hiddenEffect, Optional<Set<EffectCure>> cures) {
            this(amplifier, duration, ambient, showParticles, showIcon, hiddenEffect);
            this.cures = cures;
        }

        // but why though?
        @CreateStatic
        private static MobEffectInstance.Details create(int amplifier, int duration, boolean ambient, boolean showParticles, Optional<Boolean> showIcon, Optional<MobEffectInstance.Details> hiddenEffect, Optional<Set<EffectCure>> cures) {
            var value = new MobEffectInstance.Details(amplifier, duration, ambient, showParticles, showIcon.orElse(showParticles), hiddenEffect);
            ((MobEffectInstanceInjection.DetailsInjection) (Object) value).nytheriacompat$setCures(cures);
            return value;
        }
    }
}