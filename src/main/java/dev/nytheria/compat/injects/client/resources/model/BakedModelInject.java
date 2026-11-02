// TRACKED HASH: 38803da8adefb294d6d64e0841a02273caf2b283
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.resources.model;

import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BakedModel.class)
public interface BakedModelInject extends IBakedModelExtension {
}