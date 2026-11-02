/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.util

class IteratorWrapper<T>(private val original: MutableIterator<T>, private val modifier: (T?) -> T?) : MutableIterator<T> {
    private var nextCached: T? = null

    override fun next(): T {
        if (this.nextCached != null) {
            val value = this.nextCached!!
            this.nextCached = null
            return value
        }

        if (original.hasNext())
            return original.next()

        throw NoSuchElementException()
    }

    override fun hasNext(): Boolean {
        if (original.hasNext()) {
            val value = original.next()
            this.nextCached = modifier.invoke(value)

            return if (this.nextCached == null)
                original.hasNext()
            else
                true
        }

        return false
    }

    override fun remove() {
        original.remove()
    }
}