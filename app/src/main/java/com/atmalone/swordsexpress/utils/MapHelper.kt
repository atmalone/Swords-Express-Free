package com.atmalone.swordsexpress.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import com.atmalone.swordsexpress.R
import com.atmalone.swordsexpress.deserializers.TimetableDeserializer
import com.atmalone.swordsexpress.models.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.fragment_maps.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class MapHelper {
    companion object {

        fun createScaledBitmap(drawable: Int, context: Context): Bitmap {
            val bitmapdraw = ContextCompat.getDrawable(context, drawable) as BitmapDrawable
            val b = bitmapdraw.bitmap
            return Bitmap.createScaledBitmap(b, 100, 100, false)
        }

        fun createBusMarkers(fragment: FragmentActivity, map: GoogleMap, busMap:HashMap<String, Marker>, buses: MutableList<BusInfo>?) {
            try {
                fragment.runOnUiThread {
                    buses?.forEach { bus ->
                        var marker = createMarker(
                            bus.licenseNum,
                            LatLng(bus.lat, bus.long),
                            createScaledBitmap(R.drawable.bus_icon_green, fragment),
                            bus
                        )
//                        busMap.forEach { s, marker ->
                        val m = map.addMarker(marker)
                        busMap.put(m.title, m)
//                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

        fun getTimetableListFromResourceFile(fragment: FragmentActivity, direction: Boolean): List<Timetable> {
            val gsonBuilder = GsonBuilder().serializeNulls()
            gsonBuilder.registerTypeAdapter(
                Timetable::class.java,
                TimetableDeserializer()
            )
            val gson = gsonBuilder.create()
            val resource = selectTimetableRawResource(direction)
            val timetableRouteList = gson.fromJson(fragment.resources.openRawResource(resource)
                .bufferedReader().use {
                    it.readText()
                }, Array<Timetable>::class.java
            )
                .toList()
            return timetableRouteList
        }

        fun getNextBusAtStop(fragment: FragmentActivity, stopTitle: String, direction: Boolean): String {
            val timetableRouteList = getTimetableListFromResourceFile(fragment, direction)
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


        fun createMarker(fragment: FragmentActivity, direction: Boolean, index: String, location: LatLng, bitmap: Bitmap,
                         title: String): MarkerOptions {
            val snippetText = getNextBusAtStop(fragment, title, direction)
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

        fun createMarker(title: String, location: LatLng, bitmap: Bitmap, busInfo: BusInfo): MarkerOptions {
            val markerOption: MarkerOptions = MarkerOptions().position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .snippet(createBusMarkerSnippet(busInfo))
            return markerOption
        }

        fun createBusMarkerSnippet(busInfo: BusInfo): String {
            return "Travelling ${busInfo.direction} at ${busInfo.speed}"
        }

        fun createSwordsMarkers(fragment: FragmentActivity, direction: Boolean, map: GoogleMap, mStopHashMapToSwords: HashMap<String, Marker>,
                                isVisible: Boolean) {
            var toSwordsStops = Gson().fromJson(
                fragment.resources.openRawResource(R.raw.to_swords)
                    .bufferedReader().use { it.readText() }, Array<StopInfo>::class.java
            )

            toSwordsStops.forEach { stopInfo ->
                var markerOption = createMarker(fragment, direction,
                    stopInfo.stop_num,
                    LatLng(stopInfo.lat.toDouble(), stopInfo.long.toDouble()),
                    createScaledBitmap(R.drawable.stop_icon_small, fragment),
                    stopInfo.stop_name)
                    .visible(isVisible)
                mStopHashMapToSwords[stopInfo.stop_name] = map.addMarker(markerOption)
                map.addMarker(markerOption)
            }
        }

        fun createCityMarkers(fragment: FragmentActivity, direction: Boolean, map: GoogleMap, mStopHashMapToCity: HashMap<String, Marker>,
                              isVisible: Boolean) {
            var toCityStops = Gson().fromJson(
                fragment.resources.openRawResource(R.raw.to_city)
                    .bufferedReader().use { it.readText() }, Array<StopInfo>::class.java
            )

            toCityStops.forEach { stopInfo ->
                var markerOption = createMarker(fragment, direction,
                    stopInfo.stop_num, LatLng(stopInfo.lat.toDouble(), stopInfo.long.toDouble()),
                    createScaledBitmap(R.drawable.stop_icon_small, fragment), stopInfo.stop_name)
                    .visible(isVisible)
                mStopHashMapToCity[stopInfo.stop_name] = map.addMarker(markerOption)
                map.addMarker(markerOption)
            }
        }

        fun showHidePolyline(map: GoogleMap, fragmentActivity: FragmentActivity,
                                     polylines: MutableList<Polyline>,
                                     patternList: MutableList<PatternItem>,
                                     direction: Boolean,
                                     checkedRadioButtonText: String, 
                                     routeList: List<Route>) {
            val routes = routeList

            for (line in polylines) {
                line.remove()
            }

            polylines.clear()
            try {
                when (checkedRadioButtonText) {
                    "500" -> {
                        if (direction) {
                            val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                            val route: Route? = routes.find { it.title == "waypoints500fromCity" }
                            val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(route?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                                )
                            )
                        } else if (!direction) {
                            val routeStart: Route? = routes.find { it.title == "SwordsManorStart" }
                            val routeMid1: Route? = routes.find { it.title == "waypoints500fromSwords" }
                            val routeMid2: Route? = routes.find { it.title == "waypointsPortTunnel3Arena" }
                            val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid2?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                                )
                            )
                        }

                    }
                    "501" -> {
                        if (direction) {
                            val routeStart: Route? = routes.find { it.title == "waypointsMerrionSqToPearseSt" }
                            val route: Route? = routes.find { it.title == "waypoints500fromCity" }
                            val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(route?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                                )
                            )
                        } else if (!direction) {
                            val routeStart: Route? = routes.find { it.title == "PavilionsStart" }
                            val route: Route? = routes.find { it.title == "waypoints501fromSwords" }
                            val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(route?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                                )
                            )
                        }

                    }
                    "502" -> {
                        if (direction) {
                            val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                            val route: Route? = routes.find { it.title == "waypoints506fromCity" }
                            val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(route?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                                )
                            )
                        } else if (!direction) {
                            val routeStart: Route? = routes.find { it.title == "HighfieldStart" }
                            val route: Route? = routes.find { it.title == "waypoints501fromSwords" }
                            val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(route?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                                )
                            )
                        }
                    }
                    "503" -> {
                        if (!direction) {
                            val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                            val route: Route? = routes.find { it.title == "waypoints500XfromCity" }
                            val routeEnd: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(route?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                                )
                            )
                        } else if (direction) {
                            val routeStart: Route? = routes.find { it.title == "SwordsManorStart" }
                            val routeMid1: Route? = routes.find { it.title == "waypoints500fromSwords" }
                            val routeMid2: Route? = routes.find { it.title == "waypointsPortTunnel3Arena" }
                            val routeEnd: Route? = routes.find { it.title == "waypointsPearseGardaToMerrionSq" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid2?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                                )
                            )
                        }
                    }
                    "504" -> {
                        if (direction) {
                            val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                            val route: Route? = routes.find { it.title == "waypoints501XfromCity" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(route?.value)
                                )
                            )
                        } else if (!direction) {
                            val routeStart: Route? = routes.find { it.title == "waypoints504fromSwords" }
                            val route: Route? = routes.find { it.title == "waypointsPortTunnel3Arena" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(route?.value)
                                )
                            )
                        }
                    }
                    "505" -> {
                        if (direction) {
                            val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                            val route: Route? = routes.find { it.title == "waypoints505XfromCity" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(route?.value)
                                )
                            )
                        } else if (!direction) {
                            val routeStart: Route? = routes.find { it.title == "HighfieldStart" }
                            val routeMid1: Route? = routes.find { it.title == "waypointsRiverValleyLoop" }
                            val routeMid2: Route? = routes.find { it.title == "waypoints505fromSwords" }
                            val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid2?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                                )
                            )
                        }
                    }
                    "506" -> {
                        if (direction) {
                            val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
                            val routeMid1: Route? = routes.find { it.title == "waypoints506fromCity" }
                            val route: Route? = routes.find { it.title == "waypointsSwordsManorFinish" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(route?.value)
                                )
                            )
                        } else if (!direction) {
                            val routeStart: Route? = routes.find { it.title == "HighfieldStart" }
                            val routeMid1: Route? = routes.find { it.title == "waypoints501fromSwords" }
                            val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(
                                            routeStart?.value
                                        )
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                                )
                            )
                            polylines.add(
                                map.addPolyline(
                                    PolylineOptions()
                                        .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
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
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeStart?.value)
                            )
                        )
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                            )
                        )
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid2?.value)
                            )
                        )
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    }
                    "500X" -> {
                        val routeStart: Route? = routes.find { it.title == "SwordsManorStart" }
                        val route: Route? = routes.find { it.title == "waypoints500XfromSwords" }
                        val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeStart?.value)
                            )
                        )
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(route?.value)
                            )
                        )
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    }
                    "501X" -> {
                        val routeStart: Route? = routes.find { it.title == "PavilionsStart" }
                        val routeMid1: Route? = routes.find { it.title == "PavilionsExtensions501X" }
                        val routeMid2: Route? = routes.find { it.title == "waypoints501XfromSwords" }
                        val routeEnd: Route? = routes.find { it.title == "waypoint3ArenaQuays" }
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeStart?.value)
                            )
                        )
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid1?.value)
                            )
                        )
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid2?.value)
                            )
                        )
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    }
                    "505X" -> {
                        val routeStart: Route? = routes.find { it.title == "waypointsEdenQuayStart" }
//                val routeMid1: Route? = routes.find { it.title == "waypoints505XfromCity" }
                        val routeEnd: Route? = routes.find { it.title == "waypoints505XfromCity" }
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeStart?.value)
                            )
                        )
//                polylines.add(map.addPolyline(PolylineOptions()
//                    .pattern(mPatternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeMid1?.value)))
                        polylines.add(
                            map.addPolyline(
                                PolylineOptions()
                                    .pattern(patternList).color(fragmentActivity.getColor(R.color.colorGreen)).addAll(routeEnd?.value)
                            )
                        )
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}