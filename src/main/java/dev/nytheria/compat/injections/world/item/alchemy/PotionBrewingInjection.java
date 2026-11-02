/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item.alchemy;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.neoforged.neoforge.common.brewing.BrewingRecipeRegistry;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;

public interface PotionBrewingInjection {
    AtomicReference<RegistryAccess> nytheriacompat$registryAccess = new AtomicReference<>(RegistryAccess.EMPTY);

    static PotionBrewing bootstrap(FeatureFlagSet enabledFeatures, RegistryAccess registryAccess) {
        nytheriacompat$registryAccess.set(registryAccess);
        return PotionBrewing.bootstrap(enabledFeatures);
    }

    default boolean isInput(ItemStack stack) {
        throw NytheriaCompatHelper.createMixinException(PotionBrewingInjection.class, "isInput");
    }

    default List<IBrewingRecipe> neoforge$getRecipes() {
        throw NytheriaCompatHelper.createMixinException(PotionBrewingInjection.class, "getRecipes");
    }

    default void nytheriacompat$setBrewingRegistry(BrewingRecipeRegistry registry) {
        throw NytheriaCompatHelper.createMixinException(PotionBrewingInjection.class, "nytheriacompat$setBrewingRegistry");
    }

    interface BuilderInjection {
        default void addRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
            throw NytheriaCompatHelper.createMixinException(BuilderInjection.class, "addRecipe");
        }

        default void addRecipe(IBrewingRecipe recipe) {
            throw NytheriaCompatHelper.createMixinException(BuilderInjection.class, "addRecipe");
        }
    }
}
