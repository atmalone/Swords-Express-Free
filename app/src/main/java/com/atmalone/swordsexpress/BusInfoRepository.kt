package com.atmalone.swordsexpress

import android.util.Log
import com.atmalone.swordsexpress.models.BusInfo
import com.google.gson.Gson
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.ArrayList

class BusInfoRepository {

    private val url = "https://www.swordsexpress.com/latlong.php"

    fun getBusInfoListFromResponseBody(completion: (MutableList<BusInfo>) -> Unit): MutableList<BusInfo> {
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()
        var buses = mutableListOf<BusInfo>()
        client.newCall(request).enqueue(object : Callback {

            override fun onResponse(call: okhttp3.Call?, response: okhttp3.Response?) {
                val stringResponse = response?.body()?.string()
                buses = createBusesFromResponseBody(stringResponse)
                completion(buses)
            }

            override fun onFailure(call: okhttp3.Call?, e: IOException?) {
                Log.e("Http API call failed", e?.message)
            }
        })
        return buses
    }

    private fun createBusesFromResponseBody(busResponseBodyText: String?): MutableList<BusInfo> {
        val buses: ArrayList<BusInfo> = arrayListOf()
        if (!busResponseBodyText.isNullOrEmpty()) {
            val arrayOfBuses = Gson().fromJson(busResponseBodyText, arrayListOf<MutableList<String>>()::class.java)
            for (busArray in arrayOfBuses) {
                if (busArray[1] == "hidden") continue
                val license = busArray[0]
//                //todo delete me
//                if (lat != 0.0) {
//                    lat += 0.05
//                } else {
//                lat = busArray[1].toDouble()
                val lat = busArray[1].toDouble()
//                }
                val long: Double = busArray[2].toDouble()
                val dateTime: String = busArray[3]
                val num: String = busArray[4]
                val speed: String = busArray[5]
                var direction: String = busArray[6]

                when (direction) {
                    "n" -> direction = "North"
                    "ne" -> direction = "Northeast"
                    "nw" -> direction = "Northwest"
                    "s" -> direction = "South"
                    "se" -> direction = "Southeast"
                    "sw" -> direction = "Southwest"
                    "e" -> direction = "East"
                    "w" -> direction = "West"
                }


                var bus = BusInfo(license, lat, long, dateTime, num, speed, direction)

                buses.add(bus)
            }
        }
        return buses
    }

    companion object {
        private var sInstance: BusInfoRepository? = null
        fun instance(): BusInfoRepository {
            if (sInstance == null) {
                synchronized(BusInfoRepository) {
                    sInstance = BusInfoRepository()
                }
            }
            return sInstance!!
        }
    }

}

