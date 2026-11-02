/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.loader.mixin.modifications

import org.objectweb.asm.Type

data class ParamPair(
    val descriptor: String,
    val ordinal: Int
) {
    constructor(type: Type, ordinal: Int) : this(type.descriptor, ordinal)
}
