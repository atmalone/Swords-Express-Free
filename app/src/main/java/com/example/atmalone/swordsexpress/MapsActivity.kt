package com.example.atmalone.swordsexpress

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
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
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.experimental.launch
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


class MapsActivity : Fragment(), OnMapReadyCallback {
    private val url = "https://www.swordsexpress.com/latlong.php"

    private lateinit var mMap: GoogleMap
    private lateinit var toSwordsStops: Array<StopInfo>
    private lateinit var toCityStops: Array<StopInfo>
    //    private var buses = ArrayList<BusInfo>()
    private var mHandler = Handler()
    private lateinit var mRunnable: Runnable
    var lat = 0.0

    var direction: String = "city"
    private val mStopMap = HashMap<String, Marker>()
    private val mBusMap = HashMap<String, Marker>()
    private var busResponseBody: String? = ""

    val REQUEST_READ_EXTERNAL = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_maps, container, false)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        launch {
            fetchBusDataFromUrl()
        }

        toggleStops()
    }

    private fun createBusesFromResponseBody(busResponseBodyText: String?): List<BusInfo>? {
        if (!busResponseBodyText.isNullOrEmpty()) {

            val arrayOfBuses = Gson().fromJson(busResponseBodyText, arrayListOf<MutableList<String>>()::class.java)

            val buses: ArrayList<BusInfo> = arrayListOf()

            for (busArray in arrayOfBuses) {
                if (busArray[1] == "hidden") continue
                val license = busArray[0]
                //todo delete me
                if (lat != 0.0) {
                    lat += 0.05
                } else {
                    lat = busArray[1].toDouble()

                }
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

            return buses
        }

        return null
    }

    private fun fetchBusDataFromUrl() {


        val request = Request.Builder().url(url).build()

        var client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: okhttp3.Call?, response: okhttp3.Response?) {
                busResponseBody = response?.body()?.string()

                var buses = createBusesFromResponseBody(busResponseBody)

                if (buses?.size == 0) {
                    //todo delete me
                    val busString =
                        resources.openRawResource(R.raw.buses)
                            .bufferedReader().use { it.readText() }

                    buses = createBusesFromResponseBody(busString)


                    buses?.let { createBusMarkers(it) }

                } else {

                    //todo keep this
                    buses?.let {
                        createBusMarkers(it)
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call?, e: IOException?) {

            }
        })
    }

    private fun createScaledBitmap(drawable: Int): Bitmap {
        val bitmapdraw = ContextCompat.getDrawable(this.requireContext(), drawable) as BitmapDrawable
        val b = bitmapdraw.bitmap
        return Bitmap.createScaledBitmap(b, 100, 100, false)
    }

    private fun createBusMarkers(buses: List<BusInfo>) {
        try {
            buses.forEach { bus ->
                requireActivity().runOnUiThread {
                    mBusMap[bus.licenseNum] = createMarker(
                        bus.licenseNum,
                        LatLng(bus.lat, bus.long),
                        createScaledBitmap(R.drawable.bus_icon_green),
                        bus.licenseNum
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun updateBusMarkers() {
        mBusMap.forEach { s, marker ->
            requireActivity().runOnUiThread {
                marker.remove()
            }
        }
        mBusMap.clear()
//        buses.removeAll(buses)

//        mRunnable = Runnable {
        fetchBusDataFromUrl()

        Log.i("update-", "The runnable 'createBusMarkers' was hit")
//        }
//        mHandler.post(mRunnable)
        Log.i("update", "The runnable 'post runnable' was hit")
    }

//    private fun updateBusMarkers() {
//        try {
//            mBusMap.clear()
//        }
//        catch (e: Exception)
//        {
//            Log.i("remove buses from Map", e.toString())
//            e.printStackTrace() }
//        try {
//            while(buses.size != 0) {
//                buses.removeAll(buses)
//            }
//        } catch (e: Exception) {
//            Log.i("remove buses", e.toString())
//            e.printStackTrace()
//        }
//
//        launch {
//            fetchBusDataFromUrl()
//        }
//        mRunnable = Runnable {
//            createBusMarkers(buses)
//            Log.i("update-", "The runnable 'createBusMarkers' was hit")
//        }
//        mHandler.post(mRunnable)
//    }

    private fun createSwordsMarkers() {
        toSwordsStops = Gson().fromJson(
            resources.openRawResource(R.raw.to_swords)
                .bufferedReader().use { it.readText() }, Array<StopInfo>::class.java
        )

        toSwordsStops.forEach { stopInfo ->
            mStopMap[stopInfo.stop_num] = createMarker(
                stopInfo.stop_num,
                LatLng(stopInfo.lat.toDouble(), stopInfo.long.toDouble()),
                createScaledBitmap(R.drawable.stop_icon_small),
                stopInfo.stop_name
            )
        }
    }

    private fun createCityMarkers() {
        toCityStops = Gson().fromJson(
            resources.openRawResource(R.raw.to_city)
                .bufferedReader().use { it.readText() }, Array<StopInfo>::class.java
        )

        toCityStops.forEach { stopInfo ->
            mStopMap[stopInfo.stop_num] = createMarker(
                stopInfo.stop_num,
                LatLng(stopInfo.lat.toDouble(), stopInfo.long.toDouble()),
                createScaledBitmap(R.drawable.stop_icon_small),
                stopInfo.stop_name
            )
        }
    }

    private fun createMarker(index: String, location: LatLng, bitmap: Bitmap, title: String): Marker {
        val markerOption = MarkerOptions().position(location)
            .title(title)
            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
        return mMap.addMarker(markerOption)
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
        val radioGroup: RadioGroup = direction_group
        val swordsRadioButton: RadioButton = swords
        val cityRadioButton: RadioButton = city

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            var text = "You selected:"
            when {
                (cityRadioButton.isChecked) -> {
                    direction = "city"
                    text += direction

                    mStopMap.forEach { stop, marker ->
                        marker.remove()
                    }

                    mStopMap.entries.clear()
                    createCityMarkers()
                }
                (swordsRadioButton.isChecked) -> {
                    direction = "swords"
                    text += direction
                    mStopMap.forEach { stop, marker ->
                        marker.remove()
                    }
                    mStopMap.entries.clear()
                    createSwordsMarkers()
                }
            }
            Toast.makeText(this.context, text, Toast.LENGTH_SHORT).show()
        }
    }
}