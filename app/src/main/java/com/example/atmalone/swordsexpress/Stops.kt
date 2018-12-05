package com.example.atmalone.swordsexpress

import android.support.v7.app.AppCompatActivity
import com.google.gson.Gson

class Stops : AppCompatActivity() {

    var stops : List<StopInfo>? = null

    fun getStopList(): List<StopInfo>? {
        val stops = Gson().fromJson(resources.openRawResource(R.raw.to_swords)
            .bufferedReader().use{ it.readText() } , Array<StopInfo>::class.java).toMutableList()
        return stops
    }
}