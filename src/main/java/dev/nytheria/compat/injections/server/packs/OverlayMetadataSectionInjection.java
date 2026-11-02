/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.server.packs;

import com.mojang.serialization.Codec;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.metadata.MetadataSectionType;

import java.util.List;

public interface OverlayMetadataSectionInjection {
    public static final MetadataSectionType<OverlayMetadataSection> NEOFORGE_TYPE = MetadataSectionType.fromCodec("neoforge:overlays", OverlayMetadataSection.CODEC);

    /**
     * Only tells whether a pack declares Fabric API's {@code fabric:overlays} section; the content is never looked at,
     * so the codec accepts any JSON and cannot fail (a failed decode would make the section look absent).
     */
    public static final MetadataSectionType<?> FABRIC_TYPE = MetadataSectionType.fromCodec("fabric:overlays", Codec.PASSTHROUGH);

    /**
     * Which {@code neoforge:overlays} directories of a pack Nytheria Compat adds to its overlay list.
     * <ul>
     *     <li>None for a pack that is not a NeoForge source ({@code neoForgeSource} false, see
     *     {@link dev.nytheria.compat.util.NeoForgeSources}): plain Fabric never applies {@code neoforge:overlays}, and their
     *     conditions are often Fabric ones (Lithostitched: {@code apollib:config}), which Nytheria Compat's condition codec turns
     *     into "true", so overlays the author switched off would be applied.</li>
     *     <li>None for a NeoForge source that also declares {@code fabric:overlays}: Fabric API decides its overlays with
     *     the conditions the author wrote for Fabric.</li>
     *     <li>Otherwise all of them (NeoForge mods Nytheria Compat runs, Nytheria Compat's own packs).</li>
     * </ul>
     * {@code neoForgeSource} is always true with {@code -Dnytheriacompat.neoDataFromFabricMods=true}, which leaves the second
     * rule only (the earlier behaviour).
     */
    public static List<String> neoForgeOverlaysToAdd(boolean neoForgeSource, boolean declaresFabricOverlays, List<String> neoForgeOverlays) {
        return neoForgeSource && !declaresFabricOverlays ? neoForgeOverlays : List.of();
    }
}
