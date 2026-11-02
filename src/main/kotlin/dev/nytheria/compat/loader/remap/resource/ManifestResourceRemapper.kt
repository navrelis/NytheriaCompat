/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap.resource

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.jar.Manifest

object ManifestResourceRemapper : ResourceRemapper {
    override fun canTransform(path: String): Boolean {
        return path.startsWith("META-INF") && path.endsWith("MANIFEST.MF")
    }

    override fun transform(path: String, input: InputStream): ByteArray {
        // Modify the manifest to avoid hash checking, because if
        // hash checking occurs, the JAR will fail to load entirely.
        val manifest = Manifest(input)

        // Remove SHA-256 and SHA-1 hashes
        for (key in manifest.entries.keys.toList()) {
            val keys = manifest.entries[key]!!.keys
            if (keys.any { it.toString() == "SHA-256-Digest" || it.toString() == "SHA-1-Digest" }) {
                manifest.entries.remove(key)
            }
        }

        return ByteArrayOutputStream().use {
            manifest.write(it)
            it.toByteArray()
        }
    }
}