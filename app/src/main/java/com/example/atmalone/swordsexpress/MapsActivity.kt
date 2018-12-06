package com.example.atmalone.swordsexpress

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import java.util.*
import android.graphics.Bitmap
import android.provider.MediaStore.Images.Media.getBitmap
import android.graphics.drawable.BitmapDrawable
import com.google.android.gms.maps.model.BitmapDescriptorFactory


class MapsActivity : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var marker: Marker
    private lateinit var stops: Array<StopInfo>

    private val mStopMap = HashMap<String, Marker>()

    val REQUEST_READ_EXTERNAL = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_maps, container, false)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        getListOfStops()
//        createMarkers()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_READ_EXTERNAL) getListOfStops()
    }

    private fun getListOfStops() {
        stops = Gson().fromJson(resources.openRawResource(R.raw.to_swords)
            .bufferedReader().use { it.readText() }, Array<StopInfo>::class.java)
    }

    fun createMarkers() {
        val bitmapdraw = resources.getDrawable(R.drawable.stop_icon_small) as BitmapDrawable
        val b = bitmapdraw.bitmap
        val smallMarker = Bitmap.createScaledBitmap(b, 100, 100, false)

        stops.forEach { stopInfo ->
            val stopPosition = LatLng(stopInfo.lat.toDouble(), stopInfo.long.toDouble())
            val markerOption = MarkerOptions().position(stopPosition)
                .title(stopInfo.stop_name)
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
            marker = mMap.addMarker(markerOption)
            mStopMap[stopInfo.stop_num] = marker
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        createMarkers()

        val stop = stops.first()
        val startPosition = LatLng(stop.lat.toDouble(), stop.long.toDouble())
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, 15f))
    }
}
