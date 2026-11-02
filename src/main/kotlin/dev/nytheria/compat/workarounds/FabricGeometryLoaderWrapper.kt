/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.workarounds

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonObject
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader as PortingLibGeometryLoader
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry as PortingLibUnbakedGeometry

class FabricGeometryLoaderWrapper<T : PortingLibUnbakedGeometry<T>, U : IUnbakedGeometry<U>>(val wrapped: PortingLibGeometryLoader<T>) :
    IGeometryLoader<U> {
    override fun read(jsonObject: JsonObject?, deserializationContext: JsonDeserializationContext?): U {
        val geometry = wrapped.read(jsonObject, deserializationContext)
        return UnbakedGeometryWrapper(geometry) as U
    }
}