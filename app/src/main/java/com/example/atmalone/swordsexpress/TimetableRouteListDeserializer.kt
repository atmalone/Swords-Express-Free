package com.example.atmalone.swordsexpress

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

class TimetableRouteListDeserializer: JsonDeserializer<TimetableRoute> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): TimetableRoute {
        val jsonObj = json as JsonObject

        val timetableRoute = TimetableRoute()

        val jsonRouteDirectionTitle = jsonObj.get("title")
        val jsonRouteStopsTitle = jsonObj.getAsJsonArray("value")

        try {
            timetableRoute.title = jsonRouteDirectionTitle.asString
            jsonRouteStopsTitle.forEach { routeStop ->
                var routestopTitle = routeStop.asJsonObject.get("title").asString
                timetableRoute.value.add(routestopTitle)
            }
        }
        catch (e: Exception){
            Log.e("RouteSerialiser","failed to serialise routes.")
            e.printStackTrace()
        }
        return timetableRoute
    }
}