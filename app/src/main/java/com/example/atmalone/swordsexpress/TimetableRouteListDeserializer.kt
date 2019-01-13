package com.example.atmalone.swordsexpress

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

class TimetableRouteListDeserializer: JsonDeserializer<TimetableRouteTitle> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): TimetableRouteTitle {
        val jsonObj = json as JsonObject

        val timetableRoute = TimetableRouteTitle()

        val jsonRouteDirectionTitle = jsonObj.get("title")
        val jsonRouteStopsTitle = jsonObj.getAsJsonArray("value")

        try {
            timetableRoute.title = jsonRouteDirectionTitle.asString
            jsonRouteStopsTitle.forEach { routeStop ->
                var routeStopTitle = routeStop.asJsonObject.get("title").asString
                timetableRoute.value.add(routeStopTitle)
            }
        }
        catch (e: Exception){
            Log.e("TimetableDeserializer","failed to serialise routes.")
            e.printStackTrace()
        }
        return timetableRoute
    }
}