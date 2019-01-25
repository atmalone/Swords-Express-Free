package com.atmalone.swordsexpress

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
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
import com.google.android.gms.ads.AdView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tapadoo.alerter.Alerter
import kotlinx.android.synthetic.main.fragment_maps.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.joda.time.chrono.GregorianChronology
import org.joda.time.format.DateTimeFormat
import java.io.IOException
import java.time.format.FormatStyle
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.scheduleAtFixedRate

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private val url = "https://www.swordsexpress.com/latlong.php"

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mMap: GoogleMap
    private lateinit var toSwordsStops: Array<StopInfo>
    private lateinit var toCityStops: Array<StopInfo>
    private var mHandler = Handler()
    private lateinit var mRunnable: Runnable
//    var lat = 0.0
    var mToSwords: Boolean = true
    private val mStopMap = HashMap<String, Marker>()
    var polylines = mutableListOf<Polyline>()
    private val mBusMap = HashMap<String, Marker>()
    private var busResponseBody: String? = ""
    val pattern: PatternItem = Dot()
    val patternList = mutableListOf<PatternItem>()
    private val TAG = "Location Permission"
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var mAdView : AdView
    private var clickedMarkerTitle: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this.requireActivity())

        setupPermissions()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.requireActivity())
        return inflater.inflate(R.layout.fragment_maps, container, false)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mAdView = Helpers.adHelper(view, requireContext())
        supportMapFragment.getMapAsync(this)

        GlobalScope.launch {
            fetchBusDataFromUrl()
            getRouteObjectsFromJsonArray()
            toggleStops()
        }
        patternList.add(pattern)
    }

    private fun setupPermissions() {
        if (ContextCompat.checkSelfPermission(this.requireContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.requireContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            Log.e(TAG, "PERMISSION GRANTED")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> {
                    setUpMap()
                }
                PackageManager.PERMISSION_DENIED -> setupPermissions()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    private fun createBusesFromResponseBody(busResponseBodyText: String?): List<BusInfo>? {
        if (!busResponseBodyText.isNullOrEmpty()) {

            val arrayOfBuses = Gson().fromJson(busResponseBodyText, arrayListOf<MutableList<String>>()::class.java)

            val buses: ArrayList<BusInfo> = arrayListOf()

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
//                    //todo delete me
//                    val busString =
//                        resources.openRawResource(R.raw.buses)
//                            .bufferedReader().use { it.readText() }
//
//                    buses = createBusesFromResponseBody(busString)
//
//
//                    buses?.let { createBusMarkers(it) }
                    if(!Alerter.isShowing)
                        Alerter.create(requireActivity())
                            .setTitle("Service Unavailable")
                            .setText("Sorry, there are no buses available at this time")
                            .enableSwipeToDismiss()
                            .enableInfiniteDuration(true)
                            .setBackgroundColorRes(R.color.colorGreen)
                            .show()

                } else {
                    if(Alerter.isShowing)
                        Alerter.clearCurrent(requireActivity())
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
                        bus.licenseNum,
                        bus.direction,
                        bus.speed
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun updateBusMarkers() {
        if(this.activity != null) {
            mBusMap.forEach { (s, marker) ->
                requireActivity().runOnUiThread {
                    marker.remove()
                }
            }
            mBusMap.clear()
            fetchBusDataFromUrl()
        }
    }

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

    private fun selectTimetableRawResource(mToSwords: Boolean) : Int {
        var weekSelectionRawResource = 0
        val calendar = DateTime()
        val day: Int = calendar.dayOfWeek

        when {
            (mToSwords && day in 1..5) -> {
                weekSelectionRawResource = R.raw.city_swords_mon_fri
            }
            (mToSwords && day == 6) -> {
                weekSelectionRawResource = R.raw.city_swords_sat
            }
            (mToSwords && day == 0) -> {
                weekSelectionRawResource = R.raw.city_swords_sun
            }
            (!mToSwords && day in 1..5) -> {
                weekSelectionRawResource = R.raw.swords_city_mon_fri
            }
            (!mToSwords && day == 6) -> {
                weekSelectionRawResource = R.raw.swords_city_sat
            }
            (!mToSwords && day == 0) -> {
                weekSelectionRawResource = R.raw.swords_city_sun
            }
        }
        return weekSelectionRawResource
    }

    private fun getTimetableListFromResourceFile(): List<Timetable> {
        val gsonBuilder = GsonBuilder().serializeNulls()
        gsonBuilder.registerTypeAdapter(Timetable::class.java, TimetableDeserializer())
        val gson = gsonBuilder.create()
        val resource = selectTimetableRawResource(mToSwords)
        val timetableRouteList = gson.fromJson(resources.openRawResource(resource)
            .bufferedReader().use {
                it.readText()
            }, Array<Timetable>::class.java)
            .toList()
        return timetableRouteList
    }

    fun getNextBusAtStop(stopTitle: String): String {
        val timetableRouteList = getTimetableListFromResourceFile()
        val timetable = timetableRouteList.find {
            it.title == stopTitle
        }

        val formatter = DateTimeFormat.forPattern("HH:mm")
        val currentTime = DateTime.now().toString(formatter)
        var nextBusString = ""

        try {
            val timetableItem:TimetableItem? = timetable?.values?.find {
                DateTime.parse(currentTime, formatter) <=
                        DateTime.parse(it.time, formatter)
            }
            if (timetableItem?.route != null || timetableItem?.time != null)
                nextBusString = "Next expected bus is ${timetableItem.route} at ${timetableItem.time}"

        }catch (e:Exception){
            e.printStackTrace()
        }
        return nextBusString
    }

    private fun createMarker(index: String, location: LatLng, bitmap: Bitmap, title: String): Marker {
        val snippetText = getNextBusAtStop(title)
        val markerOption : MarkerOptions
        if (snippetText != ""){
            markerOption = MarkerOptions().position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .snippet(snippetText)
        } else {
            markerOption = MarkerOptions().position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
        }
        return mMap.addMarker(markerOption)
    }

    private fun createMarker(index: String, location: LatLng, bitmap: Bitmap, title: String, heading: String?, speed: String?): Marker {
        val markerOption = MarkerOptions().position(location)
            .title(title)
            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            ?.snippet("Travelling $heading at $speed")
        return mMap.addMarker(markerOption)
    }

    private fun setUpMap() {
        if (ContextCompat.checkSelfPermission(this.requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener(this.requireActivity()) { location ->

                if (location != null) {
                    lastLocation = location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
            return
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

        createCityMarkers()

        val stop = toCityStops.last()
        val startPosition = LatLng(stop.lat.toDouble(), stop.long.toDouble())
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, 15f))

        updateBusesPeriodically()

        val routes = getRouteObjectsFromJsonArray()
        val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
        val route: Route? = routes.find { it.title == "waypoints500fromCity" }
        val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
        polylines.add(this.mMap.addPolyline(PolylineOptions()
            .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
        polylines.add(this.mMap.addPolyline(PolylineOptions()
            .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
        polylines.add(this.mMap.addPolyline(PolylineOptions()
            .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))

        routeRadioButtonGroupListener(routes)
        setUpMap()

    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        clickedMarkerTitle = marker?.title ?: ""
        if (clickedMarkerTitle != "") {
            updateStopWithNextBusAnnotation(clickedMarkerTitle)
        }
        return false
    }


    private fun getRouteObjectsFromJsonArray(): List<Route> {
        val gsonBuilder = GsonBuilder().serializeNulls()
        gsonBuilder.registerTypeAdapter(Route::class.java, RouteDeserializer())
        val gson = gsonBuilder.create()

        val routeList = gson.fromJson(resources.openRawResource(R.raw.routes)
            .bufferedReader().use { it.readText() }, Array<Route>::class.java).toList()

        return routeList
    }

    private fun updateBusesPeriodically() {
        val timer = Timer()
        GlobalScope.launch {
            timer.scheduleAtFixedRate(10000, 10000) {
                updateBusMarkers()
            }
        }
    }

    private fun updateStopWithNextBusAnnotation(stopName: String) : String {
        var nextBusAtStop : String = ""

        return nextBusAtStop
    }

    private fun toggleStops() {
        val radioGroup: RadioGroup = direction_group
        val swordsRadioButton: RadioButton = rb_swords
        val cityRadioButton: RadioButton = rb_city

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when {
                (cityRadioButton.isChecked) -> {
                    mToSwords = false

                    mStopMap.forEach { (stop, marker) ->
                        marker.remove()
                    }

                    mStopMap.entries.clear()
                    createCityMarkers()
                }
                (swordsRadioButton.isChecked) -> {
                    mToSwords = true
                    mStopMap.forEach { (stop, marker) ->
                        marker.remove()
                    }
                    mStopMap.entries.clear()
                    createSwordsMarkers()
                }
            }
            if (cityRadioButton.isChecked){
                val routeRadioGroupToCity = requireActivity().findViewById(R.id.route_group_to_city) as RadioGroup
                val routeRadioGroupToSwords = requireActivity().findViewById(R.id.route_group_to_swords) as RadioGroup
                routeRadioGroupToSwords.visibility = View.INVISIBLE
                routeRadioGroupToCity.visibility = View.VISIBLE
            }
            if (swordsRadioButton.isChecked){
                val routeRadioGroupToCity = requireActivity().findViewById(R.id.route_group_to_city) as RadioGroup
                val routeRadioGroupToSwords = requireActivity().findViewById(R.id.route_group_to_swords) as RadioGroup
                routeRadioGroupToSwords.visibility = View.VISIBLE
                routeRadioGroupToCity.visibility = View.INVISIBLE
            }
        }
    }

    fun routeRadioButtonGroupListener(routes: List<Route>) {
        val routeRadioGroupToCity = requireActivity().findViewById(R.id.route_group_to_city) as RadioGroup
        val routeRadioGroupToSwords = requireActivity().findViewById(R.id.route_group_to_swords) as RadioGroup

        routeRadioGroupToSwords.setOnCheckedChangeListener { group, checkedId ->
            val checkedRadioButtonText = requireActivity().findViewById<RadioButton>(checkedId).text
            showHidePolyline(checkedRadioButtonText as String, routes)
        }
        routeRadioGroupToCity.setOnCheckedChangeListener { group, checkedId ->
            val checkedRadioButtonText = requireActivity().findViewById<RadioButton>(checkedId).text
            showHidePolyline(checkedRadioButtonText as String, routes)
        }
    }

    fun showHidePolyline(checkedRadioButtonText: String, routeList: List<Route>) {
        val routes = routeList

        for(line in polylines)
        {
            line.remove()
        }

        polylines.clear()
    try{
        when (checkedRadioButtonText){
             "500" -> {
                 if (mToSwords){
                     val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                     val route: Route? = routes.find { it.title == "waypoints500fromCity" }
                     val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                     polylines.add(this.mMap.addPolyline(PolylineOptions()
                         .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                     polylines.add(this.mMap.addPolyline(PolylineOptions()
                         .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
                     polylines.add(this.mMap.addPolyline(PolylineOptions()
                         .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
                 }
                 else if(!mToSwords) {
                     val routeStart: Route? = routes.find { it.title == "SwordsManorStart" }
                     val routeMid1: Route? = routes.find { it.title == "waypoints500fromSwords" }
                     val routeMid2: Route? = routes.find { it.title == "waypointsPortTunnel3Arena" }
                     val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                     polylines.add(this.mMap.addPolyline(PolylineOptions()
                         .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                     polylines.add(this.mMap.addPolyline(PolylineOptions()
                         .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)))
                     polylines.add(this.mMap.addPolyline(PolylineOptions()
                         .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid2?.value)))
                     polylines.add(this.mMap.addPolyline(PolylineOptions()
                         .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
                 }

             }
            "501" -> {
                if (mToSwords){
                    val routeStart: Route? = routes.find { it.title == "waypointsMerrionSqToPearseSt" }
                    val route: Route? = routes.find { it.title == "waypoints500fromCity" }
                    val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
                } else if(!mToSwords) {
                    val routeStart: Route? = routes.find { it.title == "PavilionsStart" }
                    val route: Route? = routes.find { it.title == "waypoints501fromSwords" }
                    val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
                    }

                }
            "502" -> {
                if (mToSwords){
                    val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                    val route: Route? = routes.find { it.title == "waypoints506fromCity" }
                    val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
                } else if(!mToSwords) {
                    val routeStart: Route? = routes.find { it.title == "HighfieldStart" }
                    val route: Route? = routes.find { it.title == "waypoints501fromSwords" }
                    val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
                }
            }
            "503" -> {
                if (!mToSwords){
                    val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                    val route: Route? = routes.find { it.title == "waypoints500XfromCity" }
                    val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
                } else if(mToSwords) {
                    val routeStart: Route? = routes.find { it.title == "SwordsManorStart" }
                    val routeMid1: Route? = routes.find { it.title == "waypoints500fromSwords" }
                    val routeMid2: Route? = routes.find { it.title == "waypointsPortTunnel3Arena" }
                    val routeEnd: Route? = routes.find { it.title == "waypointsPearseGardaToMerrionSq" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid2?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
                }
            }
            "504" -> {
                if (mToSwords) {
                    val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                    val route: Route? = routes.find { it.title == "waypoints501XfromCity" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
                } else if(!mToSwords) {
                    val routeStart: Route? = routes.find { it.title == "waypoints504fromSwords" }
                    val route: Route? = routes.find { it.title == "waypointsPortTunnel3Arena" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
                }
            }
            "505" -> {
                if (mToSwords) {
                    val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                    val route: Route? = routes.find { it.title == "waypoints505XfromCity" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
                } else if(!mToSwords) {
                    val routeStart: Route? = routes.find { it.title == "HighfieldStart" }
                    val routeMid1: Route? = routes.find { it.title == "waypointsRiverValleyLoop" }
                    val routeMid2: Route? = routes.find { it.title == "waypoints505fromSwords" }
                    val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid2?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
                }
            }
            "506" -> {
                if (mToSwords) {
                    val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                    val routeMid1: Route? = routes.find { it.title == "waypoints506fromCity" }
                    val route: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
                } else if(!mToSwords) {
                    val routeStart: Route? = routes.find { it.title == "HighfieldStart" }
                    val routeMid1: Route? = routes.find { it.title == "waypoints501fromSwords" }
                    val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)))
                    polylines.add(this.mMap.addPolyline(PolylineOptions()
                        .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
                }
            }
            "507" -> {
                val routeStart: Route? = routes.find { it.title == "SwordsManorStart" }
                val routeMid1: Route? = routes.find { it.title == "waypoints507fromSwords" }
                val routeMid2: Route? = routes.find { it.title == "waypointsPortTunnel3Arena" }
                val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)))
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid2?.value)))
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
            }
            "500X" -> {
                val routeStart: Route? = routes.find { it.title == "SwordsManorStart" }
                val route: Route? = routes.find { it.title == "waypoints500XfromSwords" }
                val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)))
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
            }
            "501X" -> {
                val routeStart: Route? = routes.find { it.title == "PavilionsStart" }
                val routeMid1: Route? = routes.find { it.title == "PavilionsExtensions501X" }
                val routeMid2: Route? = routes.find { it.title == "waypoints501XfromSwords" }
                val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)))
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid2?.value)))
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
            }
            "505X" -> {
                val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
//                val routeMid1: Route? = routes.find { it.title == "waypoints505XfromCity" }
                val routeEnd: Route? = routes.find { it.title == "waypoints505XfromCity" }
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)))
//                polylines.add(this.mMap.addPolyline(PolylineOptions()
//                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)))
                polylines.add(this.mMap.addPolyline(PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)))
            }

        }
    } catch (e: Exception){
        e.printStackTrace()
    }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
