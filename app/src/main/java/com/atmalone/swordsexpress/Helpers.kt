package com.atmalone.swordsexpress

import android.content.Context
import android.view.View
import android.widget.Adapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.gson.GsonBuilder

class Helpers {
    companion object {
        fun adHelper(view: View, context: Context): AdView {
            var mAdView = view.findViewById(R.id.adView) as AdView
            val adRequest = AdRequest.Builder()
                .addTestDevice("C50314F746E1C4158AFF27F3CAAD5DF5")
                .build()
            mAdView.loadAd(adRequest)

            adRequest.isTestDevice(context)
            return mAdView
        }
    }
}