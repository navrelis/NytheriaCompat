/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class FallbackDelegate<T : Any>(private val fallbackProvider: () -> T) : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: fallbackProvider()
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

fun <T : Any> fallback(provider: () -> T): FallbackDelegate<T> = FallbackDelegate(provider)
