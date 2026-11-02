package dev.nytheria.compat.mixin.compat.nitrogen;

import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.extensions.IKeyMappingExtension;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Nytheria Compat: The Aether / Nitrogen Internals for Fabric injects its own copy of NeoForge's key mapping extension into
 * {@link KeyMapping}, including a default {@code isActiveAndMatches(InputConstants.Key)}. NeoForge's
 * {@link IKeyMappingExtension} has a default method with the exact same name and descriptor, and neither is
 * implemented by {@link KeyMapping} itself. The JVM can't pick between two unrelated default methods, so every call
 * (the Aether does one on each key press in {@code KeyMapping.set}/{@code click}, NeoForge in its key lookup) throws
 * {@code IncompatibleClassChangeError: Conflicting default methods}.
 * <p>
 * Implementing the method in the class resolves that. It uses NeoForge's behaviour, which is a superset of the
 * Aether's (it also checks the key conflict context and modifier, both of which default to "always active").
 * Nitrogen Internals 1.1.22+ prefixes its copy of the method, so there it is simply the same as not having this mixin.
 */
@IfModLoaded("nitrogen_internals")
@Mixin(KeyMapping.class)
public abstract class NitrogenKeyMappingMixin implements IKeyMappingExtension {
    @Override
    public boolean isActiveAndMatches(InputConstants.Key keyCode) {
        return IKeyMappingExtension.super.isActiveAndMatches(keyCode);
    }
}
