/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap.resource

import dev.nytheria.compat.loader.remap.NytheriaCompatRemapper
import xyz.bluspring.knit.loader.mod.ModDefinition
import java.io.InputStream

object IgnoreSignatureResourceRemapper : ResourceRemapper {
    override fun canTransform(path: String): Boolean {
        return path.lowercase().endsWith(".rsa") || path.lowercase().endsWith(".sf")
    }

    override fun transform(mod: ModDefinition, path: String, input: InputStream): ByteArray? {
        // ignore JAR signatures.
        // Due to Nytheria Compat remapping the JAR files, we are unable to use this to our advantage.
        // TODO: Maybe run a verification step in the mod loading process prior to remapping?
        NytheriaCompatRemapper.logger.warn("Detected that ${mod.displayName} (${mod.id}) is a signed JAR! This is a security measure by mod developers to verify that the distributed mod JARs are theirs, however Nytheria Compat is unable to use this verification step properly, and is thus stripping this information.")

        return null
    }
}