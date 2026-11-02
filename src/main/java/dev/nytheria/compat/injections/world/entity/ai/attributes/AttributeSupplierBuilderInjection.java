/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.entity.ai.attributes;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import dev.nytheria.compat.mixin.AttributeSupplierAccessor;
import dev.nytheria.compat.mixin.AttributeSupplierBuilderAccessor;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.NytheriaCompatHelper;

@FabricInjectedInterface(AttributeSupplier.Builder.class)
public interface AttributeSupplierBuilderInjection {
    static AttributeSupplier.Builder create(AttributeSupplier attributeMap) {
        var builder = new AttributeSupplier.Builder();
        ((AttributeSupplierBuilderAccessor) builder).getBuilder().putAll(((AttributeSupplierAccessor) attributeMap).getInstances());

        return builder;
    }

    default void combine(AttributeSupplier.Builder other) {
        throw NytheriaCompatHelper.createMixinException(AttributeSupplierBuilderInjection.class, "combine");
    }

    default boolean hasAttribute(Holder<Attribute> attribute) {
        throw NytheriaCompatHelper.createMixinException(AttributeSupplierBuilderInjection.class, "hasAttribute");
    }
}
