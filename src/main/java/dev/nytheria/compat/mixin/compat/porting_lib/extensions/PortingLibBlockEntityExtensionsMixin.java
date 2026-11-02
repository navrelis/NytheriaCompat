/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.mixin.compat.porting_lib.extensions;

import io.github.fabricators_of_create.porting_lib.blocks.extensions.CustomDataPacketHandlingBlockEntity;
import io.github.fabricators_of_create.porting_lib.blocks.extensions.CustomRenderBoundingBoxBlockEntity;
import io.github.fabricators_of_create.porting_lib.blocks.extensions.CustomUpdateTagHandlingBlockEntity;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({
    CustomDataPacketHandlingBlockEntity.class,
    CustomRenderBoundingBoxBlockEntity.class,
    CustomUpdateTagHandlingBlockEntity.class,
})
public interface PortingLibBlockEntityExtensionsMixin extends IBlockEntityExtension {
}
