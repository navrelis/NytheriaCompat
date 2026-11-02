/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.util

import net.fabricmc.api.EnvType
import net.neoforged.api.distmarker.Dist

object DistUtil {
    fun distToEnvType(dist: Dist): EnvType {
        return when (dist) {
            Dist.CLIENT -> EnvType.CLIENT
            Dist.DEDICATED_SERVER -> EnvType.SERVER
            else -> throw IllegalStateException()
        }
    }

    fun envTypeToDist(envType: EnvType): Dist {
        return when (envType) {
            EnvType.CLIENT -> Dist.CLIENT
            EnvType.SERVER -> Dist.DEDICATED_SERVER
            else -> throw IllegalStateException()
        }
    }

    fun isDistEqual(dist: Dist, envType: EnvType): Boolean {
        return distToEnvType(dist) == envType
    }
}