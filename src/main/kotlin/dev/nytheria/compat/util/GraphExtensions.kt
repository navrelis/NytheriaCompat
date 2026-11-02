/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.util

import com.google.common.graph.GraphBuilder
import com.google.common.graph.MutableGraph
import net.neoforged.neoforgespi.language.IModInfo
import dev.nytheria.compat.loader.mod.NeoForgeMod

@Suppress("UnstableApiUsage")
fun Collection<NeoForgeMod>.buildGraph(): MutableGraph<NeoForgeMod> {
    val graph = GraphBuilder.directed().build<NeoForgeMod>()

    for (mod in this) {
        graph.addNode(mod)
    }

    for (mod in this) {
        for (dep in mod.dependencies) {
            val associatedMod = firstOrNull { it.modId == dep.modId } ?: continue

            if (associatedMod == mod)
                continue

            if (dep.ordering == IModInfo.Ordering.BEFORE) {
                graph.putEdge(mod, associatedMod)
            } else if (dep.ordering == IModInfo.Ordering.AFTER) {
                graph.putEdge(associatedMod, mod)
            }
        }
    }
    return graph
}