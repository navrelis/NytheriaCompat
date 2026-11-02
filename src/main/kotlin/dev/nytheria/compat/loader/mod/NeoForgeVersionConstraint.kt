/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.mod

import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.VersionRange
import xyz.bluspring.knit.loader.mod.VersionConstraint

class NeoForgeVersionConstraint(val range: VersionRange) : VersionConstraint {
    override fun matches(versionString: String): Boolean {
        if (versionString == "0.0NONE") // Forge, what the fuck?
            return true

        return range.containsVersion(DefaultArtifactVersion(versionString))
    }

    override fun toString(): String {
        return range.toString()
    }
}