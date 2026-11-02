/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.remap.resource

import xyz.bluspring.knit.loader.mod.ModDefinition
import java.io.InputStream

interface ResourceRemapper {
    fun canTransform(path: String): Boolean

    fun transform(mod: ModDefinition, path: String, input: InputStream): ByteArray? {
        return transform(path, input)
    }

    fun transform(path: String, input: InputStream): ByteArray? {
        return null
    }
}