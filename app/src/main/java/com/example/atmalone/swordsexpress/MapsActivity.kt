package com.example.atmalone.swordsexpress

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.*
import kotlinx.coroutines.experimental.launch
import kotlin.collections.ArrayList
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.math.log


class MapsActivity : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var marker: Marker
    private lateinit var toSwordsStops: Array<StopInfo>
    private lateinit var toCityStops: Array<StopInfo>
    private var buses = ArrayList<BusInfo>()
    private lateinit var busBitMap : Bitmap
    private var mHandler = Handler()
    private lateinit var mRunnable: Runnable


    var direction: String = "city"
    private val mStopMap = HashMap<String, Marker>()
    private val mBusMap = HashMap<String, Marker>()
    private var busResponseBody : String? = ""

    val REQUEST_READ_EXTERNAL = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_maps, container, false)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        fetchBusDataFromUrl()

        launch {
            createBusBitmap()
            toggleStops()
        }
    }

    private fun createBusesFromResponseBody(busResponseBodyText : String?) {
        if (!busResponseBodyText.isNullOrEmpty()){

            val arrayOfBuses = Gson().fromJson(busResponseBodyText, arrayListOf<MutableList<String>>()::class.java)
            var bus : BusInfo

            for (busArray in arrayOfBuses) {
                if (busArray[1] == "hidden") continue
                val license = busArray[0]
                val lat: Double = busArray[1].toDouble()
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


                bus = BusInfo(license, lat, long, dateTime, num, speed, direction)

                buses.add(bus)
            }
        }
    }

    private fun fetchBusDataFromUrl() {

        val url : String = "https://www.swordsexpress.com/latlong.php"

        val request = Request.Builder().url(url).build()

        var client = OkHttpClient()
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: okhttp3.Call?, response: okhttp3.Response?) {
                busResponseBody = response?.body()?.string()

                launch {
                    createBusesFromResponseBody(busResponseBody)
                }
            }
            override fun onFailure(call: okhttp3.Call?, e: IOException?) {

            }
        })
    }

    private fun createBusBitmap() {
        val bitmapdraw = resources.getDrawable(R.drawable.bus_icon_green) as BitmapDrawable
        val b = bitmapdraw.bitmap
        busBitMap = Bitmap.createScaledBitmap(b, 100, 100, false)
    }

    private fun createBusMarkers(buses: ArrayList<BusInfo>){
        try{
        buses.forEach { bus ->
            val busPosition = LatLng(bus.lat, bus.long)
            val markerOption = MarkerOptions().position(busPosition)
                .title(bus.licenseNum)
                .icon(BitmapDescriptorFactory.fromBitmap(busBitMap))
            marker = mMap.addMarker(markerOption)
            mBusMap[bus.licenseNum] = marker
            }
        }
        catch (e: Exception){
            e.printStackTrace()
        }

    }

    private fun updateBusMarkers() {
        mBusMap.clear()
        buses.removeAll(buses)

        fetchBusDataFromUrl()
        mRunnable = Runnable {
            createBusMarkers(buses)
            Log.i("uodate-", "The runnable 'createBusMarkers' was hit")
        }
        mHandler.postAtFrontOfQueue(mRunnable)
        Log.i("uodate", "The runnable 'post runnable' was hit")
    }

    private fun createSwordsMarkers() {
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

    private fun createCityMarkers() {
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
        createBusMarkers(buses)

        val stop = toSwordsStops.first()
        val startPosition = LatLng(stop.lat.toDouble(), stop.long.toDouble())
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, 15f))

        updateBusesPeriodically()

    }

    fun updateBusesPeriodically() {
        val timer = Timer()
        launch {
            timer.scheduleAtFixedRate(10000, 10000) {
                updateBusMarkers()
            }
        }
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
