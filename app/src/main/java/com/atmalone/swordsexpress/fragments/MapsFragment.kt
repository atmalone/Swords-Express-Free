package com.atmalone.swordsexpress.fragments

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
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
import com.atmalone.swordsexpress.utils.Helpers
import com.atmalone.swordsexpress.models.*
import com.atmalone.swordsexpress.R
import com.atmalone.swordsexpress.utils.MapHelper
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
import com.tapadoo.alerter.Alerter
import kotlinx.android.synthetic.main.fragment_maps.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.scheduleAtFixedRate

class MapsFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mMap: GoogleMap
    private var mToSwords: Boolean = true
    private val mStopMapToSwords = HashMap<String, Marker>()
    private val mStopMapToCity = HashMap<String, Marker>()
    private var mPolylines = mutableListOf<Polyline>()
    private var mBusMap = HashMap<String, Marker>()
    private val pattern: PatternItem = Dot()
    private val mPatternList = mutableListOf<PatternItem>()
    private val TAG = "Location Permission"
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mAdView: AdView
    private lateinit var mBusInfoViewModel: BusInfoViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this.requireActivity())
        setupPermissions()
        if (isAdded){
            mBusInfoViewModel = ViewModelProviders.of(this.requireActivity()).get(BusInfoViewModel::class.java)
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.requireActivity())
        return inflater.inflate(R.layout.fragment_maps, container, false)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mAdView = Helpers.adHelper(view, requireContext())
        supportMapFragment.getMapAsync(this)
        mPatternList.add(pattern)
        if (isAdded){
            mBusInfoViewModel.reposResult.observe(this, android.arch.lifecycle.Observer{
            })
        }
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

    private fun createAlerter(buses: MutableList<BusInfo>?) {
        if (buses?.size == 0) {
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
        MapHelper.createCityMarkers(requireActivity(), mToSwords, mMap, mStopMapToCity,false)
        MapHelper.createSwordsMarkers(requireActivity(), mToSwords, mMap, mStopMapToSwords,false)
        createDirectionRadioGroupClickListener()
        getUserLastLocation()

        val routes = Helpers.getRouteObjectsFromJsonArray(requireActivity())
        val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
        val route: Route? = routes.find { it.title == "waypoints500fromCity" }
        val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
        mPolylines.add(
            this.mMap.addPolyline(
                PolylineOptions()
                    .pattern(mPatternList).color(resources.getColor(R.color.colorGreen)).addAll(routeStart?.value)
            )
        )
        mPolylines.add(
            this.mMap.addPolyline(
                PolylineOptions()
                    .pattern(mPatternList).color(resources.getColor(R.color.colorGreen)).addAll(route?.value)
            )
        )
        mPolylines.add(
            this.mMap.addPolyline(
                PolylineOptions()
                    .pattern(mPatternList).color(resources.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
            )
        )

        routeRadioButtonGroupListener(routes)

        mStopMapToCity.forEach { (stop, marker) ->
            marker.isVisible = true
        }

        MapHelper.createBusMarkers(requireActivity(),mMap, mBusMap, mBusInfoViewModel.getBusInfoList())
        createAlerter(mBusInfoViewModel.getBusInfoList())
        updateBusesPeriodically()

    }

    private fun updateBusesPeriodically() {
        val timer = Timer()
        GlobalScope.launch {
            timer.scheduleAtFixedRate(5000, 5000) {
                updateBusMarkers()
            }
        }
    }

    private fun updateBusMarkers() {
        mBusInfoViewModel.loadBusInfoListFromUrl()
        createAlerter(mBusInfoViewModel.getBusInfoList())
        val busInfo = mBusInfoViewModel.getBusInfoList()
        requireActivity().runOnUiThread {
            mBusMap.forEach{(s, marker) ->
                val oldMarker = marker
                val newBusInfo= busInfo?.find { oldMarker.title == it.licenseNum }
                if (newBusInfo != null) {
                    val startPosition: LatLng = oldMarker.position
                    val newPosition = LatLng(newBusInfo.lat, newBusInfo.long)
                    if (startPosition != newPosition){
                        val handler = Handler(requireContext().mainLooper)
                        val start: Long = SystemClock.uptimeMillis()
                        val durationInMs = 3000F

                        handler.post {
                            var elapsed: Long
                            var t: Float
                            try {
                                val runnable: Runnable = object : Runnable {
                                    override fun run() {
                                        elapsed = SystemClock.uptimeMillis() - start;
                                        t = elapsed / durationInMs;
                                        val currentPosition = LatLng(
                                            startPosition.latitude * (1 - t) + newPosition.latitude * t,
                                            startPosition.longitude * (1 - t) + newPosition.longitude * t
                                        )
                                        if(!isAdded)
                                            return
                                        else{
                                            requireActivity().runOnUiThread {
                                                oldMarker.position = currentPosition
                                                oldMarker.snippet = MapHelper.createBusMarkerSnippet(newBusInfo)
                                            }
                                            // Repeat till progress is complete.
                                            if (t < 1) {
                                                // Post again 16ms later.
                                                handler.postDelayed(this, 16)
                                            }
                                        }
                                    }
                                }
                                Thread(runnable).start()
                            }catch (e: Exception)
                            {
                                e.printStackTrace()
                            }
                        }
                    }
                }
//                else {
//                    mBusMap.remove(oldMarker.title, oldMarker)
//                }
            }
        }
    }

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
            MapHelper.showHidePolyline(mMap, requireActivity(), mPolylines, mPatternList, mToSwords,
                checkedRadioButtonText as String, routes)
        }
        routeRadioGroupToCity.setOnCheckedChangeListener { group, checkedId ->
            val checkedRadioButtonText = requireActivity().findViewById<RadioButton>(checkedId).text
            MapHelper.showHidePolyline(mMap, requireActivity(), mPolylines, mPatternList, mToSwords,
                checkedRadioButtonText as String, routes)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
