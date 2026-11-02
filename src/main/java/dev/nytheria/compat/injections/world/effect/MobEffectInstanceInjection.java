/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.effect;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.EffectCure;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MobEffectInstanceInjection {
    default Set<EffectCure> neoforge$getCures() {
        throw NytheriaCompatHelper.createMixinException(MobEffectInstanceInjection.class, "neoforge$getCures");
    }

    interface DetailsInjection {
        Optional<Set<EffectCure>> cures();
        void nytheriacompat$setCures(Optional<Set<EffectCure>> cures);
    }
}
