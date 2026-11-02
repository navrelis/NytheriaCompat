// TRACKED HASH: cdd5dc9b62b6a3aac9dd7ef2ad87f4bcba5a3d86
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.block.model;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.client.renderer.block.model.ItemTransformsInjection;

@Mixin(ItemTransforms.class)
public class ItemTransformsInject implements ItemTransformsInjection {
    public ImmutableMap<ItemDisplayContext, ItemTransform> moddedTransforms = ImmutableMap.of();

    public ItemTransformsInject(ItemTransform thirdPersonLeftHand, ItemTransform thirdPersonRightHand, ItemTransform firstPersonLeftHand, ItemTransform firstPersonRightHand, ItemTransform head, ItemTransform gui, ItemTransform ground, ItemTransform fixed) {}

    @CreateInitializer
    public ItemTransformsInject(ItemTransform thirdPersonLeftHand, ItemTransform thirdPersonRightHand, ItemTransform firstPersonLeftHand, ItemTransform firstPersonRightHand, ItemTransform head, ItemTransform gui, ItemTransform ground, ItemTransform fixed, ImmutableMap<ItemDisplayContext, ItemTransform> moddedTransforms) {
        this(thirdPersonLeftHand, thirdPersonRightHand, firstPersonLeftHand, firstPersonRightHand, head, gui, ground, fixed);
        this.moddedTransforms = moddedTransforms;
    }

    @Override
    public ImmutableMap<ItemDisplayContext, ItemTransform> nytheriacompat$getModdedTransforms() {
        return this.moddedTransforms;
    }

    @Override
    public void nytheriacompat$setModdedTransforms(ImmutableMap<ItemDisplayContext, ItemTransform> moddedTransforms) {
        this.moddedTransforms = moddedTransforms;
    }

    @Mixin(ItemTransforms.Deserializer.class)
    public static abstract class DeserializerInject {
        @Shadow protected abstract ItemTransform getTransform(JsonDeserializationContext deserializationContext, JsonObject json, ItemDisplayContext displayContext);

    }
}