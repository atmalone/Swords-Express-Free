package com.atmalone.swordsexpress.deserializers

import android.util.Log
import com.atmalone.swordsexpress.models.Route
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

class RouteDeserializer : JsonDeserializer<Route> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Route {
        val jsonObj = json as JsonObject

        val route = Route()

        val jsonRouteTitle = jsonObj.get("title")
        val jsonCoordinatesArray = jsonObj.getAsJsonArray("value")
        try {
            route.title = jsonRouteTitle.asString
            jsonCoordinatesArray.forEach { point ->
                var routeLatLng = LatLng(point.asJsonObject.get("lat").asDouble, point.asJsonObject.get("lng").asDouble)
                route.value.add(routeLatLng)
            }
        } catch (e: Exception){
            Log.e("RouteSerialiser","failed to serialise routes.")
            e.printStackTrace()
        }
        return route
    }
}