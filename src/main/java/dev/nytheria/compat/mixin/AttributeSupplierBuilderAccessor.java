/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(AttributeSupplier.Builder.class)
public interface AttributeSupplierBuilderAccessor {
    @Accessor
    ImmutableMap.Builder<Holder<Attribute>, AttributeInstance> getBuilder();

    @Accessor
    boolean isInstanceFrozen();

    @Accessor
    void setInstanceFrozen(boolean instanceFrozen);

    @Invoker
    AttributeInstance callCreate(Holder<Attribute> attribute);
}
