/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.renderer.block;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.client.renderer.block.BlockModelShaperInjection;

@Mixin(BlockModelShaper.class)
public abstract class BlockModelShaperInject implements BlockModelShaperInjection {
    @Shadow public abstract BakedModel getBlockModel(BlockState state);

    @Override
    public TextureAtlasSprite getTexture(BlockState state, Level level, BlockPos pos) {
        var data = level.getModelDataManager().getAt(pos);
        var model = this.getBlockModel(state);
        return model.getParticleIcon(model.getModelData(level, pos, state, data == null ? ModelData.EMPTY : data));
    }
}
