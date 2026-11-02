/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.util

import org.slf4j.LoggerFactory

// A very crude implementation of a layered logging system built specifically for trying to locate PoseStack problems.
// This is not built to be efficient, only to be functional.
class LayeredStackTracker {
    private val logger = LoggerFactory.getLogger("LayeredStackTracker")

    private val layers = mutableListOf<Layer>()
    private var currentLayer = ArrayDeque<Layer>()

    fun push(source: String) {
        val layer = Layer(currentLayer.size, source)
        layers.add(layer)
        currentLayer.addLast(layer)
    }

    fun pop(source: String) {
        if (currentLayer.isEmpty()) {
            logger.warn("$source attempted to pop layer but there were no more layers!")
            return
        }

        layers.add(currentLayer.removeLast().apply {
            this.end = source
        })
    }

    fun dump() {
        logger.error("Dumping current layers!")

        val seenLayers = mutableListOf<Layer>()
        for (layer in layers) {
            if (seenLayers.contains(layer)) {
                logger.error("${"x".repeat(layer.index + 1)} ${layer.end}")
            } else {
                logger.error("${"-".repeat(layer.index + 1)} ${layer.start}")
                seenLayers.add(layer)
            }
        }
    }

    private data class Layer(val index: Int, val start: String, var end: String? = null)
}