/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.mod

import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import xyz.bluspring.knit.loader.mod.ModVersion

class NeoForgeModVersion(val forgeVersion: ArtifactVersion) : ModVersion {
    override fun toString(): String = forgeVersion.toString()

    override fun compareTo(other: ModVersion): Int {
        return forgeVersion.compareTo(DefaultArtifactVersion(other.toString()))
    }
}