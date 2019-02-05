package com.atmalone.swordsexpress.utils

import android.content.Context
import android.support.v4.app.FragmentActivity
import android.view.View
import com.atmalone.swordsexpress.R
import com.atmalone.swordsexpress.deserializers.RouteDeserializer
import com.atmalone.swordsexpress.models.Route
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.gson.GsonBuilder

class Helpers {
    companion object {
        fun adHelper(view: View, context: Context): AdView {
            val mAdView = view.findViewById(R.id.adView) as AdView
            val adRequest = AdRequest.Builder()
                .addTestDevice("C50314F746E1C4158AFF27F3CAAD5DF5")
                .build()
            mAdView.loadAd(adRequest)

            adRequest.isTestDevice(context)
            return mAdView
        }

        fun getRouteObjectsFromJsonArray(fragment: FragmentActivity): List<Route> {
            val gsonBuilder = GsonBuilder().serializeNulls()
            gsonBuilder.registerTypeAdapter(Route::class.java, RouteDeserializer())
            val gson = gsonBuilder.create()

            val routeList = gson.fromJson(
                fragment.resources.openRawResource(R.raw.routes)
                    .bufferedReader().use { it.readText() }, Array<Route>::class.java
            ).toList()

            return routeList
        }
    }
}