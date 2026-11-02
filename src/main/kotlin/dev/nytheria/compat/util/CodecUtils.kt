/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.util

import dev.nytheria.compat.shaded.cichlidmc.tinycodecs.api.codec.Codec
import dev.nytheria.compat.shaded.cichlidmc.tinycodecs.api.codec.map.MapCodec
import java.util.Locale.getDefault

inline fun <reified T : Enum<T>> enumThrowingFallbackCodec(ignoreCase: Boolean = true): Codec<T> {
    return if (ignoreCase) {
        Codec.STRING.xmap(
            { enumValueOf<T>(it.uppercase()) },
            { it.name.lowercase(getDefault()) }
        )
    } else {
        Codec.STRING.xmap(
            { enumValueOf<T>(it) },
            { it.name }
        )
    }
}

inline fun <K : Any, V : Any> unboundedMap(keyCodec: Codec<K>, valueCodec: Codec<V>): Codec<Map<K, V>> {
    return MapCodec.map(keyCodec, valueCodec).asCodec()
}
