package com.atmalone.swordsexpress

import android.view.View
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

class Helpers {
    companion object {
        fun fragmentAdHelper(view: View): AdView {
            var mAdView = view.findViewById(R.id.adView) as AdView
            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
            return mAdView
        }
    }
}