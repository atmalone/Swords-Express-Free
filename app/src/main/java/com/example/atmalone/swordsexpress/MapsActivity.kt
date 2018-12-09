package com.example.atmalone.swordsexpress

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.telecom.Call
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.google.android.gms.common.api.Response
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class MapsActivity : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var marker: Marker
    private lateinit var toSwordsStops: Array<StopInfo>
    private lateinit var toCityStops: Array<StopInfo>
    private lateinit var buses: ArrayList<BusInfo>
    private val mBusMap = HashMap<String, Marker>()

    var direction: String = "city"
    private val mStopMap = HashMap<String, Marker>()
    private val mSBusMap = HashMap<String, Marker>()

    val REQUEST_READ_EXTERNAL = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_maps, container, false)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        toggleStops()
        fetchBusJson()
    }

    private fun fetchBusJson() {

        val url : String = "https://www.swordsexpress.com/latlong.php"

        val request = Request.Builder().url(url).build()

        var client = OkHttpClient()
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: okhttp3.Call?, response: okhttp3.Response?) {
                var body = response?.body()?.string()
                print(body)

                val busesJson = Gson().fromJson(body, Array<BusInfo>::class.java)
                busesJson.forEach {busInfo ->
                    print(busInfo)
                    val busPosition = LatLng(busInfo.lat!!, busInfo.long!!)
                    val markerOption = MarkerOptions().position(busPosition)
                        .title(busInfo.licenseNum)
                    marker = mMap.addMarker(markerOption)
                    mBusMap[busInfo.licenseNum] = marker

                }
            }

            override fun onFailure(call: okhttp3.Call?, e: IOException?) {
            }
        })
    }


    fun createSwordsMarkers() {
        toSwordsStops = Gson().fromJson(resources.openRawResource(R.raw.to_swords)
            .bufferedReader().use { it.readText() }, Array<StopInfo>::class.java)

        val bitmapdraw = resources.getDrawable(R.drawable.stop_icon_small) as BitmapDrawable
        val b = bitmapdraw.bitmap
        val smallMarker = Bitmap.createScaledBitmap(b, 100, 100, false)

        toSwordsStops.forEach { stopInfo ->
            val stopPosition = LatLng(stopInfo.lat.toDouble(), stopInfo.long.toDouble())
            val markerOption = MarkerOptions().position(stopPosition)
                .title(stopInfo.stop_name)
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
            marker = mMap.addMarker(markerOption)
            mStopMap[stopInfo.stop_num] = marker
        }
    }

    fun createCityMarkers() {
        toCityStops = Gson().fromJson(resources.openRawResource(R.raw.to_city)
            .bufferedReader().use { it.readText() }, Array<StopInfo>::class.java)

        val bitmapdraw = resources.getDrawable(R.drawable.stop_icon_small) as BitmapDrawable
        val b = bitmapdraw.bitmap
        val smallMarker = Bitmap.createScaledBitmap(b, 100, 100, false)

        toCityStops.forEach { stopInfo ->
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

        createSwordsMarkers()

        val stop = toSwordsStops.first()
        val startPosition = LatLng(stop.lat.toDouble(), stop.long.toDouble())
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, 15f))
    }

    fun toggleStops() {
        val radioGroup : RadioGroup = view!!.findViewById(R.id.direction_group)
        val swordsRadioButton: RadioButton = view!!.findViewById(R.id.swords)
        val cityRadioButton: RadioButton = view!!.findViewById(R.id.city)

        radioGroup?.setOnCheckedChangeListener { group, checkedId ->
            var text = "You selected:"
            when {
                (cityRadioButton.isChecked) -> { direction = "city"
                    text += direction
                    mStopMap.entries.clear()
                    createCityMarkers()
                }
                (swordsRadioButton.isChecked) -> { direction = "swords"
                    text += direction
                    mStopMap.entries.clear()
                    createSwordsMarkers()
                }
            }
            Toast.makeText(this.context, text, Toast.LENGTH_SHORT).show()
        }
    }
}
