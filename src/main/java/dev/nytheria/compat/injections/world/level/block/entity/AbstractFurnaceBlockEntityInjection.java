/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.level.block.entity;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.impl.content.registry.FuelRegistryImpl;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.jetbrains.annotations.Nullable;
import dev.nytheria.compat.mixin.compat.fabric_api.FuelRegistryImplAccessor;

import java.util.function.ObjIntConsumer;

public interface AbstractFurnaceBlockEntityInjection {
    Object2IntMap<ItemLike> nytheriacompat$itemCookTimes = new Object2IntLinkedOpenHashMap<>();
    Object2IntMap<TagKey<Item>> nytheriacompat$tagCookTimes = new Object2IntLinkedOpenHashMap<>();

    static void buildFuels(ObjIntConsumer<Either<Item, TagKey<Item>>> fuelConsumer) {
        nytheriacompat$itemCookTimes.forEach((item, time) -> fuelConsumer.accept(Either.left(item.asItem()), time));
        nytheriacompat$tagCookTimes.forEach((tag, time) -> fuelConsumer.accept(Either.right(tag), time));
    }
}
