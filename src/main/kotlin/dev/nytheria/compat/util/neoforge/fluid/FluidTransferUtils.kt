/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.util.neoforge.fluid

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil

object FluidTransferUtils {
    @JvmStatic
    fun Long.toMillibuckets(): Int {
        return TransferUtil.truncateLong(this / 81)
    }

    @JvmStatic
    fun Long.toMillibucketsLong(): Long {
        return this / 81
    }

    @JvmStatic
    fun Int.toDroplets(): Long {
        return this * 81L
    }

    @JvmStatic
    fun Long.toDroplets(): Long {
        return this * 81L
    }
}