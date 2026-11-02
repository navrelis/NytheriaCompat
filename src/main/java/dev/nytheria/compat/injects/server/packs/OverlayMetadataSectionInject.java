/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.packs;

import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.server.packs.OverlayMetadataSectionInjection;


@Mixin(OverlayMetadataSection.class)
public abstract class OverlayMetadataSectionInject {

    @CreateStatic private static final MetadataSectionType<OverlayMetadataSection> NEOFORGE_TYPE = OverlayMetadataSectionInjection.NEOFORGE_TYPE;
}
