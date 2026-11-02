// TRACKED HASH: c3fbda8033be117044af2f7df8b9938868361aaa
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.packs.metadata.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.injections.server.packs.metadata.pack.PackMetadataSectionInjection;

import java.util.Map;
import java.util.Optional;

@Mixin(PackMetadataSection.class)
public abstract class PackMetadataSectionInject implements PackMetadataSectionInjection {
    public PackMetadataSectionInject(Component description, int packFormat, Optional<InclusiveRange<Integer>> supportedFormats) {
    }

    @CreateInitializer
    public PackMetadataSectionInject(Component description, int packFormat) {
        this(description, packFormat, Optional.empty());
    }
}