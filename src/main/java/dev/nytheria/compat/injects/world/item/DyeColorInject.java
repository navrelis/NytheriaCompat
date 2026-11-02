/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.tags.ItemTagsInjection;
import dev.nytheria.compat.injections.world.item.DyeColorInjection;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.MapColor;

@Mixin(DyeColor.class)
public abstract class DyeColorInject implements DyeColorInjection {
    @Unique private TagKey<Item> dyesTag;
    @Unique private TagKey<Item> dyedTag;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nytheriacompat$createTagsForDyeName(String string, int i, int id, String name, int textureDefuseColor, MapColor mapColor, int fireworkColor, int textColor, CallbackInfo ci) {
        this.dyesTag = ItemTagsInjection.create(ResourceLocation.fromNamespaceAndPath("c", "dyes/" + name));
        this.dyedTag = ItemTagsInjection.create(ResourceLocation.fromNamespaceAndPath("c", "dyed/" + name));
    }

    @Override
    public TagKey<Item> getTag() {
        return dyesTag;
    }

    @CreateStatic
    private static DyeColor getColor(ItemStack stack) {
        return DyeColorInjection.getColor(stack);
    }
}
