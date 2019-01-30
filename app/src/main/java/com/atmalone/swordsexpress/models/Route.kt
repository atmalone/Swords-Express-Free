package com.atmalone.swordsexpress.models

import com.google.android.gms.maps.model.LatLng

class Route() {
    var title: String = ""
    var value : MutableList<LatLng> = ArrayList()
}

class Coordinates() {
    var lat: Double = 0.0
    var lng: Double = 0.0
}