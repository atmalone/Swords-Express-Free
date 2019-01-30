package com.atmalone.swordsexpress.fragments

import android.Manifest
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RadioButton
import android.widget.RadioGroup
import com.atmalone.swordsexpress.deserializers.TimetableDeserializer
import com.atmalone.swordsexpress.utils.Helpers
import com.atmalone.swordsexpress.models.*
import com.atmalone.swordsexpress.R
import com.atmalone.swordsexpress.deserializers.RouteDeserializer
import com.atmalone.swordsexpress.viewmodels.BusInfoViewModel
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
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.scheduleAtFixedRate

class MapsFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mMap: GoogleMap
    private lateinit var toSwordsStops: Array<StopInfo>
    private lateinit var toCityStops: Array<StopInfo>
    //    var lat = 0.0
    var mToSwords: Boolean = true
    private val mStopMapToSwords = HashMap<String, Marker>()
    private val mStopMapToCity = HashMap<String, Marker>()
    var polylines = mutableListOf<Polyline>()
    private var mBusMap = HashMap<String, MarkerOptions>()
    private var mBusHashMapUpdated = HashMap<String, MarkerOptions>()
    val pattern: PatternItem = Dot()
    val patternList = mutableListOf<PatternItem>()
    private val TAG = "Location Permission"
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var mAdView: AdView
    private lateinit var mModel: BusInfoViewModel
    val busInfoViewModel: BusInfoViewModel
        get() = ViewModelProviders.of(this).get(BusInfoViewModel::class.java)


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this.requireActivity())
        setupPermissions()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.requireActivity())

        mModel = ViewModelProviders.of(this).get(BusInfoViewModel::class.java)

        return inflater.inflate(R.layout.fragment_maps, container, false)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mAdView = Helpers.adHelper(view, requireContext())
        supportMapFragment.getMapAsync(this)
//        GlobalScope.launch{
//            fetchBusDataFromUrlAsync(mBusMap)
//            var listOfBuses = mModel.loadBusInfoListFromUrl()
//        }
        patternList.add(pattern)
    }


    private fun setupPermissions() {
        if (ContextCompat.checkSelfPermission(
                this.requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this.requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            Log.e(TAG, "PERMISSION GRANTED")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> {
                    getUserLastLocation()
                }
                PackageManager.PERMISSION_DENIED -> setupPermissions()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun createScaledBitmap(drawable: Int): Bitmap {
        val bitmapdraw = ContextCompat.getDrawable(this.requireContext(), drawable) as BitmapDrawable
        val b = bitmapdraw.bitmap
        return Bitmap.createScaledBitmap(b, 100, 100, false)
    }

    private fun createAlerter(buses: MutableList<BusInfo>) {
        if (buses.size == 0) {
            if (!Alerter.isShowing)
                Alerter.create(requireActivity())
                    .setTitle("Service Unavailable")
                    .setText("Sorry, there are no buses available at this time")
                    .enableSwipeToDismiss()
                    .enableInfiniteDuration(true)
                    .setBackgroundColorRes(R.color.colorGreen)
                    .show()

        } else {
            if (Alerter.isShowing)
                Alerter.clearCurrent(requireActivity())
        }
    }

    private fun createBusMarkers(busMap:HashMap<String, MarkerOptions>,buses: MutableLiveData<MutableList<BusInfo>>) {
        try {
            requireActivity().runOnUiThread {
                buses.value?.forEach { bus ->
                    busMap[bus.licenseNum] = createMarker(
                        bus.licenseNum,
                        LatLng(bus.lat, bus.long),
                        createScaledBitmap(R.drawable.bus_icon_green),
                        bus.licenseNum,
                        bus.direction,
                        bus.speed
                    )
                    busMap.forEach { s, marker ->
                        mMap.addMarker(marker)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createSwordsMarkers(isVisible: Boolean) {
        toSwordsStops = Gson().fromJson(
            resources.openRawResource(R.raw.to_swords)
                .bufferedReader().use { it.readText() }, Array<StopInfo>::class.java
        )

        toSwordsStops.forEach { stopInfo ->
            var markerOption = createMarker(
                stopInfo.stop_num, LatLng(stopInfo.lat.toDouble(), stopInfo.long.toDouble()),
                createScaledBitmap(R.drawable.stop_icon_small), stopInfo.stop_name
            ).visible(isVisible)
            mStopMapToSwords[stopInfo.stop_name] = mMap.addMarker(markerOption)
            mMap.addMarker(markerOption)
        }
    }

    private fun createCityMarkers(isVisible: Boolean) {
        toCityStops = Gson().fromJson(
            resources.openRawResource(R.raw.to_city)
                .bufferedReader().use { it.readText() }, Array<StopInfo>::class.java
        )

        toCityStops.forEach { stopInfo ->
            var markerOption = createMarker(
                stopInfo.stop_num, LatLng(stopInfo.lat.toDouble(), stopInfo.long.toDouble()),
                createScaledBitmap(R.drawable.stop_icon_small), stopInfo.stop_name
            ).visible(isVisible)
            mStopMapToCity[stopInfo.stop_name] = mMap.addMarker(markerOption)
            mMap.addMarker(markerOption)
        }
    }

    private fun selectTimetableRawResource(mToSwords: Boolean): Int {
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
            (mToSwords && day == 7) -> {
                weekSelectionRawResource = R.raw.city_swords_sun
            }
            (!mToSwords && day in 1..5) -> {
                weekSelectionRawResource = R.raw.swords_city_mon_fri
            }
            (!mToSwords && day == 6) -> {
                weekSelectionRawResource = R.raw.swords_city_sat
            }
            (!mToSwords && day == 7) -> {
                weekSelectionRawResource = R.raw.swords_city_sun
            }
        }
        return weekSelectionRawResource
    }

    private fun getTimetableListFromResourceFile(): List<Timetable> {
        val gsonBuilder = GsonBuilder().serializeNulls()
        gsonBuilder.registerTypeAdapter(Timetable::class.java,
            TimetableDeserializer()
        )
        val gson = gsonBuilder.create()
        val resource = selectTimetableRawResource(mToSwords)
        val timetableRouteList = gson.fromJson(resources.openRawResource(resource)
            .bufferedReader().use {
                it.readText()
            }, Array<Timetable>::class.java
        )
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
            val timetableItem: TimetableItem? = timetable?.values?.find {
                DateTime.parse(currentTime, formatter) <=
                        DateTime.parse(it.time, formatter)
            }
            if (timetableItem?.route != null || timetableItem?.time != null)
                nextBusString = "Next expected bus is ${timetableItem.route} at ${timetableItem.time}"

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return nextBusString
    }

    private fun createMarker(index: String, location: LatLng, bitmap: Bitmap, title: String): MarkerOptions {
        val snippetText = getNextBusAtStop(title)
        val markerOption: MarkerOptions
        if (snippetText != "") {
            markerOption = MarkerOptions().position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .snippet(snippetText)
        } else {
            markerOption = MarkerOptions().position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
        }
        return markerOption
    }

    private fun createMarker(index: String, location: LatLng, bitmap: Bitmap, title: String, heading: String?,
                             speed: String?): MarkerOptions {
        val markerOption: MarkerOptions = MarkerOptions().position(location)
            .title(title)
            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            .snippet("Travelling $heading at $speed")
        return markerOption
    }

    private fun getUserLastLocation() {
        if (ContextCompat.checkSelfPermission(
                this.requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        createCityMarkers(false)
        createSwordsMarkers(false)
        getRouteObjectsFromJsonArray()
        createDirectionRadioGroupClickListener()
        getUserLastLocation()

//        val stop = toCityStops.last()
//        val startPosition = LatLng(stop.lat.toDouble(), stop.long.toDouble())
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, 15f))


        val routes = getRouteObjectsFromJsonArray()
        val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
        val route: Route? = routes.find { it.title == "waypoints500fromCity" }
        val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
        polylines.add(
            this.mMap.addPolyline(
                PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)
            )
        )
        polylines.add(
            this.mMap.addPolyline(
                PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
            )
        )
        polylines.add(
            this.mMap.addPolyline(
                PolylineOptions()
                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
            )
        )

        routeRadioButtonGroupListener(routes)
        mStopMapToCity.forEach { (stop, marker) ->
            marker.isVisible = true
        }

        busInfoViewModel.reposResult.observe(this, android.arch.lifecycle.Observer {
            createBusMarkers(mBusMap, busInfoViewModel.reposResult)
        })
        updateBusesPeriodically()

    }

    private fun getRouteObjectsFromJsonArray(): List<Route> {
        val gsonBuilder = GsonBuilder().serializeNulls()
        gsonBuilder.registerTypeAdapter(Route::class.java, RouteDeserializer())
        val gson = gsonBuilder.create()

        val routeList = gson.fromJson(resources.openRawResource(R.raw.routes)
            .bufferedReader().use { it.readText() }, Array<Route>::class.java
        ).toList()

        return routeList
    }

    private fun updateBusesPeriodically() {
        val timer = Timer()
        GlobalScope.launch {
            timer.scheduleAtFixedRate(10000, 10000) {
//                if (mBusMap.size != 0) {
                    updateBusMarkers()
//                }
            }
        }
    }

    private fun updateBusMarkers() {

        var buses = busInfoViewModel.reposResult
        createBusMarkers(mBusHashMapUpdated, buses)
        requireActivity().runOnUiThread {
            mBusHashMapUpdated.forEach{(s, marker) ->
                var newMarker = marker
                var oldMarker = mBusMap[marker.title]
                if (oldMarker != null) {
                    var startPosition = oldMarker.position
                    var finalPosition = newMarker.position
                    if (startPosition != finalPosition){
                        val handler = Handler(requireContext().mainLooper)
                        val start: Long = SystemClock.uptimeMillis()
                        val interpolator = AccelerateDecelerateInterpolator()
                        val durationInMs = 3000F

                        handler.post {
                            var elapsed: Long
                            var t: Float
                            var v: Float

                            var runnable: Runnable = object : Runnable {
                                override fun run() {
                                    elapsed = SystemClock.uptimeMillis() - start;
                                    t = elapsed / durationInMs;
                                    v = interpolator.getInterpolation(t)
                                    var currentPosition = LatLng(
                                        startPosition.latitude * (1 - t) + finalPosition.latitude * t,
                                        startPosition.longitude * (1 - t) + finalPosition.longitude * t
                                    )
                                    requireActivity().runOnUiThread {
                                        newMarker.position(currentPosition)
                                    }
                                    // Repeat till progress is complete.
                                    if (t < 1) {
                                        // Post again 16ms later.
                                        handler.postDelayed(this, 16)
                                    }
                                }
                            }
                            Thread(runnable).start()
                        }
                    }
                }
            }
        }
    }


    //todo delete the two method below
//    override fun onMarkerClick(marker: Marker?): Boolean {
//        clickedMarkerTitle = marker?.title ?: ""
//        if (clickedMarkerTitle != "") {
//            updateStopWithNextBusAnnotation(clickedMarkerTitle)
//        }
//        return false
//    }
//
//
//    private fun updateStopWithNextBusAnnotation(stopName: String) : String {
//        var nextBusAtStop : String = ""
//
//        return nextBusAtStop
//    }

    private fun createDirectionRadioGroupClickListener() {
        val radioGroup: RadioGroup = direction_group
        val swordsRadioButton: RadioButton = rb_swords
        val cityRadioButton: RadioButton = rb_city

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when {
                (cityRadioButton.isChecked) -> {
                    mToSwords = false

                    mStopMapToSwords.forEach { (stop, marker) ->
                        marker.isVisible = false
                    }

                    mStopMapToCity.forEach { (stop, marker) ->
                        marker.isVisible = true
                    }

                }
                (swordsRadioButton.isChecked) -> {
                    mToSwords = true

                    mStopMapToSwords.forEach { (stop, marker) ->
                        marker.isVisible = true
                    }

                    mStopMapToCity.forEach { (stop, marker) ->
                        marker.isVisible = false
                    }

                }
            }
            if (cityRadioButton.isChecked) {
                val routeRadioGroupToCity = requireActivity().findViewById(R.id.route_group_to_city) as RadioGroup
                val routeRadioGroupToSwords = requireActivity().findViewById(R.id.route_group_to_swords) as RadioGroup
                routeRadioGroupToSwords.visibility = View.INVISIBLE
                routeRadioGroupToCity.visibility = View.VISIBLE
            }
            if (swordsRadioButton.isChecked) {
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

        for (line in polylines) {
            line.remove()
        }

        polylines.clear()
        try {
            when (checkedRadioButtonText) {
                "500" -> {
                    if (mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                        val route: Route? = routes.find { it.title == "waypoints500fromCity" }
                        val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    } else if (!mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "SwordsManorStart" }
                        val routeMid1: Route? = routes.find { it.title == "waypoints500fromSwords" }
                        val routeMid2: Route? = routes.find { it.title == "waypointsPortTunnel3Arena" }
                        val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid2?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    }

                }
                "501" -> {
                    if (mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "waypointsMerrionSqToPearseSt" }
                        val route: Route? = routes.find { it.title == "waypoints500fromCity" }
                        val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    } else if (!mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "PavilionsStart" }
                        val route: Route? = routes.find { it.title == "waypoints501fromSwords" }
                        val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    }

                }
                "502" -> {
                    if (mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                        val route: Route? = routes.find { it.title == "waypoints506fromCity" }
                        val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    } else if (!mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "HighfieldStart" }
                        val route: Route? = routes.find { it.title == "waypoints501fromSwords" }
                        val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    }
                }
                "503" -> {
                    if (!mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                        val route: Route? = routes.find { it.title == "waypoints500XfromCity" }
                        val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    } else if (mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "SwordsManorStart" }
                        val routeMid1: Route? = routes.find { it.title == "waypoints500fromSwords" }
                        val routeMid2: Route? = routes.find { it.title == "waypointsPortTunnel3Arena" }
                        val routeEnd: Route? = routes.find { it.title == "waypointsPearseGardaToMerrionSq" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid2?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    }
                }
                "504" -> {
                    if (mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                        val route: Route? = routes.find { it.title == "waypoints501XfromCity" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
                            )
                        )
                    } else if (!mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "waypoints504fromSwords" }
                        val route: Route? = routes.find { it.title == "waypointsPortTunnel3Arena" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
                            )
                        )
                    }
                }
                "505" -> {
                    if (mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                        val route: Route? = routes.find { it.title == "waypoints505XfromCity" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
                            )
                        )
                    } else if (!mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "HighfieldStart" }
                        val routeMid1: Route? = routes.find { it.title == "waypointsRiverValleyLoop" }
                        val routeMid2: Route? = routes.find { it.title == "waypoints505fromSwords" }
                        val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid2?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    }
                }
                "506" -> {
                    if (mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                        val routeMid1: Route? = routes.find { it.title == "waypoints506fromCity" }
                        val route: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
                            )
                        )
                    } else if (!mToSwords) {
                        val routeStart: Route? = routes.find { it.title == "HighfieldStart" }
                        val routeMid1: Route? = routes.find { it.title == "waypoints501fromSwords" }
                        val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(
                                        routeStart?.value
                                    )
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                            )
                        )
                        polylines.add(
                            this.mMap.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    }
                }
                "507" -> {
                    val routeStart: Route? = routes.find { it.title == "SwordsManorStart" }
                    val routeMid1: Route? = routes.find { it.title == "waypoints507fromSwords" }
                    val routeMid2: Route? = routes.find { it.title == "waypointsPortTunnel3Arena" }
                    val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)
                        )
                    )
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                        )
                    )
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid2?.value)
                        )
                    )
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                        )
                    )
                }
                "500X" -> {
                    val routeStart: Route? = routes.find { it.title == "SwordsManorStart" }
                    val route: Route? = routes.find { it.title == "waypoints500XfromSwords" }
                    val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)
                        )
                    )
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
                        )
                    )
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                        )
                    )
                }
                "501X" -> {
                    val routeStart: Route? = routes.find { it.title == "PavilionsStart" }
                    val routeMid1: Route? = routes.find { it.title == "PavilionsExtensions501X" }
                    val routeMid2: Route? = routes.find { it.title == "waypoints501XfromSwords" }
                    val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)
                        )
                    )
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                        )
                    )
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid2?.value)
                        )
                    )
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                        )
                    )
                }
                "505X" -> {
                    val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
//                val routeMid1: Route? = routes.find { it.title == "waypoints505XfromCity" }
                    val routeEnd: Route? = routes.find { it.title == "waypoints505XfromCity" }
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)
                        )
                    )
//                polylines.add(this.mMap.addPolyline(PolylineOptions()
//                    .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeMid1?.value)))
                    polylines.add(
                        this.mMap.addPolyline(
                            PolylineOptions()
                                .pattern(patternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                        )
                    )
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
