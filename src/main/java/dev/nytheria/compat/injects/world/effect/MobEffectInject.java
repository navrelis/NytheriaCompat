// TRACKED HASH: 77d21c29ec548ed8806ff46ca5b80c8a57b615ce
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.effect;

import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.common.extensions.IMobEffectExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.RecordMixinRefMaps;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.world.effect.MobEffectInjection;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

@Mixin(MobEffect.class)
public abstract class MobEffectInject implements MobEffectInjection, IMobEffectExtension {

    @Override
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
    }

    @Mixin(MobEffect.AttributeTemplate.class)
    public abstract static class AttributeTemplateInject implements MobEffectInjection.AttributeTemplateInjection {
        public AttributeTemplateInject(ResourceLocation id, double amount, AttributeModifier.Operation operation) {}

        @CreateInitializer
        public AttributeTemplateInject(ResourceLocation id, double amount, AttributeModifier.Operation operation, @Nullable Int2DoubleFunction curve) {
            this(id, amount, operation);
            this.nytheriacompat$setAttributeCurve(curve);
        }

        @Override
        public Int2DoubleFunction curve() {
            if (RecordMixinRefMaps.EXTENDED_ATTRIBUTE_TEMPLATE.containsKey(this)) {
                return RecordMixinRefMaps.EXTENDED_ATTRIBUTE_TEMPLATE.get(this).curve();
            }

            return null;
        }

        @Override
        public void nytheriacompat$setAttributeCurve(Int2DoubleFunction curve) {
            RecordMixinRefMaps.EXTENDED_ATTRIBUTE_TEMPLATE.put((MobEffect.AttributeTemplate) (Object) this, new RecordMixinRefMaps.ExtendedAttributeTemplate(curve));
        }
    }
}
