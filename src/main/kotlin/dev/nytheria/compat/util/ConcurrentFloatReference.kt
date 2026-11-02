/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.util

import java.util.Collections

// ah yes, this sounds like a great way to do "atomic floats" lmao.
class ConcurrentFloatReference {
    private var current = Collections.synchronizedMap(mutableMapOf<Thread, Float>())

    fun getOrElse(value: Float): Float {
        return current[Thread.currentThread()] ?: value
    }

    fun set(value: Float) {
        current[Thread.currentThread()] = value
    }

    fun reset() {
        current.remove(Thread.currentThread())
    }
}