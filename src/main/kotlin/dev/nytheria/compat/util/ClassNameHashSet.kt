/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.util

import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet

class ClassNameHashSet : ObjectOpenCustomHashSet<String>(ClassNameStrategy) {
    object ClassNameStrategy : Hash.Strategy<String> {
        override fun equals(a: String?, b: String?): Boolean {
            return a == b || a?.replace('/', '.') == b?.replace('/', '.')
        }

        override fun hashCode(o: String?): Int {
            return o?.replace('/', '.')?.hashCode() ?: 0
        }
    }
}