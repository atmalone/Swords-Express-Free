package com.example.atmalone.swordsexpress

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

class TimetableDeserializer: JsonDeserializer<Timetable> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Timetable {
        val jsonObj = json as JsonObject

        var timetableRoute = Timetable()
        var timetableItems: MutableList<TimetableItem>? = null
        var timetableItem: TimetableItem? = null

        val jsonRouteDirectionTitle = jsonObj.get("title")
        val jsonTimetableItems = jsonObj.getAsJsonArray("values")

        try {
            timetableRoute.title = jsonRouteDirectionTitle.asString
            jsonTimetableItems.forEach { timetableJsonObject ->
                var title: String = timetableRoute.title
                var time: String = timetableJsonObject.asJsonObject.get("time").asString
                var route: String = timetableJsonObject.asJsonObject.get("route").asString
                timetableItem = TimetableItem(title,time, route)
                timetableRoute.values.add(timetableItem!!)
            }
        }
        catch (e: Exception){
            Log.e("Timetable Deserializer","failed to serialise timetable.")
            e.printStackTrace()
        }
        return timetableRoute
    }
}