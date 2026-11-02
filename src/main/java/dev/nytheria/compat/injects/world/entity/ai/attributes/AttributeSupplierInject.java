// TRACKED HASH: 39b2eb34331b57077e911b6c5e5ab0553ca207cf
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity.ai.attributes;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.world.entity.ai.attributes.AttributeSupplierBuilderInjection;
import dev.nytheria.compat.mixin.AttributeSupplierAccessor;
import dev.nytheria.compat.mixin.AttributeSupplierBuilderAccessor;

import java.util.Map;

public class AttributeSupplierInject {
    @Mixin(AttributeSupplier.Builder.class)
    public static abstract class BuilderInject implements AttributeSupplierBuilderInjection {
        @Shadow
        @Final
        private ImmutableMap.Builder<Holder<Attribute>, AttributeInstance> builder;

        @Override
        public void combine(AttributeSupplier.Builder other) {
            this.builder.putAll(((AttributeSupplierBuilderAccessor) other).getBuilder().build());
        }

        @Override
        public boolean hasAttribute(Holder<Attribute> attribute) {
            return this.builder.build().containsKey(attribute);
        }

        public BuilderInject() {}

        @CreateInitializer
        public BuilderInject(AttributeSupplier attributeMap) {
            this();

            this.builder.putAll(((AttributeSupplierAccessor) attributeMap).getInstances());
        }
    }

}