/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item.crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.CraftingHelper;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import dev.nytheria.compat.processor.FabricInjectedInterface;
import dev.nytheria.compat.util.EmptyBeforeTags;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.List;
import java.util.function.Function;

@FabricInjectedInterface(Ingredient.class)
public interface IngredientInjection {
    MapCodec<Ingredient> MAP_CODEC_NONEMPTY = CraftingHelper.makeIngredientMapCodec();
    Codec<List<Ingredient>> LIST_CODEC = MAP_CODEC_NONEMPTY.codec().listOf();
    Codec<List<Ingredient>> LIST_CODEC_NONEMPTY = LIST_CODEC.validate(list -> list.isEmpty() ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined") : DataResult.success(list));

    // Nytheria Compat: see IngredientInject#nytheriacompat$rememberItemsResolvedBeforeTags
    EmptyBeforeTags<Ingredient> EMPTY_BEFORE_TAGS = new EmptyBeforeTags<>(Ingredient::nytheriacompat$forgetItems);

    static Ingredient create(ICustomIngredient customIngredient) {
        Ingredient ingredient = new Ingredient(new Ingredient.Value[0]);
        ingredient.nytheriacompat$setCustomIngredient(customIngredient);

        return ingredient;
    }

    default boolean hasNoItems() {
        throw NytheriaCompatHelper.createMixinException(IngredientInjection.class, "hasNoItems");
    }

    default Ingredient.Value[] getValues() {
        throw NytheriaCompatHelper.createMixinException(IngredientInjection.class, "getValues");
    }

    default boolean isSimple() {
        throw NytheriaCompatHelper.createMixinException(IngredientInjection.class, "isSimple");
    }

    default ICustomIngredient getCustomIngredient() {
        throw NytheriaCompatHelper.createMixinException(IngredientInjection.class, "getCustomIngredient");
    }

    default ICustomIngredient neoforge$getCustomIngredient() {
        throw NytheriaCompatHelper.createMixinException(IngredientInjection.class, "neoforge$getCustomIngredient");
    }

    default boolean isCustom() {
        throw NytheriaCompatHelper.createMixinException(IngredientInjection.class, "isCustom");
    }

    default void nytheriacompat$setCustomIngredient(ICustomIngredient customIngredient) {
        throw NytheriaCompatHelper.createMixinException(IngredientInjection.class, "nytheriacompat$setCustomIngredient");
    }

    /** Drops the cached items (and what is derived from them), so that the next request resolves them again. */
    default void nytheriacompat$forgetItems() {
        throw NytheriaCompatHelper.createMixinException(IngredientInjection.class, "nytheriacompat$forgetItems");
    }

    interface ValueInjection {
        // Nytheria Compat: Assuming and hoping mods aren't changing this out of being a map for some weird reason.
        MapCodec<Ingredient.Value> MAP_CODEC = NeoForgeExtraCodecs.xor(MapCodec.assumeMapUnsafe(Ingredient.ItemValue.CODEC), MapCodec.assumeMapUnsafe(Ingredient.TagValue.CODEC))
            .xmap((either) -> either.map(Function.identity(), Function.identity()), value -> {
                if (value instanceof Ingredient.TagValue tagValue) {
                    return Either.right(tagValue);
                } else if (value instanceof Ingredient.ItemValue itemValue) {
                    return Either.left(itemValue);
                } else {
                    throw new UnsupportedOperationException("This is neither an item value nor a tag value.");
                }
            });
    }
}
