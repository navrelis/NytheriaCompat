/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.client.renderer.block.model;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.neoforged.neoforge.client.model.geometry.BlockGeometryBakingContext;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import java.util.function.Function;

public interface BlockModelInjection {
    default BlockGeometryBakingContext nytheriacompat$getCustomData() {
        throw NytheriaCompatHelper.createMixinException(BlockModelInjection.class, "nytheriacompat$getCustomData");
    }

    default ItemOverrides getOverrides(ModelBaker baker, BlockModel blockModel, Function<Material, TextureAtlasSprite> spriteGetter) {
        throw NytheriaCompatHelper.createMixinException(BlockModelInjection.class, "getOverrides");
    }

    default BakedModel bakeVanilla(ModelBaker baker, BlockModel model, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state, boolean guiLight3d) {
        throw NytheriaCompatHelper.createMixinException(BlockModelInjection.class, "bakeVanilla");
    }

    interface GuiLightInjection {
        default String getSerializedName() {
            throw NytheriaCompatHelper.createMixinException(GuiLightInjection.class, "getSerializedName");
        }
    }
}
