/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader

import org.apache.maven.artifact.versioning.DefaultArtifactVersion

object Constants {
    @JvmField val NEOFORGE_LOADER_VERSION = DefaultArtifactVersion("4.0.43") // 1.21.1
    @JvmField val NEOFORGE_API_VERSION = DefaultArtifactVersion("21.1.248")
}
