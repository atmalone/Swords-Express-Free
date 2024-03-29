package com.atmalone.swordsexpress.fragments


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.atmalone.swordsexpress.deserializers.TimetableRouteListDeserializer
import com.atmalone.swordsexpress.utils.Helpers
import com.atmalone.swordsexpress.models.TimetableRouteTitle
import com.atmalone.swordsexpress.R
import com.atmalone.swordsexpress.adapters.RouteListAdapter
import com.google.android.gms.ads.AdView
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_route_list.*
import kotlinx.android.synthetic.main.activity_route_list.view.*

class RouteListFragment : Fragment() {

    private var mAdapter = RouteListAdapter()
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var listOfStopsToSwords : MutableList<String>
    private lateinit var listOfStopsToCity: MutableList<String>
    var to_swords: Boolean = false
    private lateinit var mContext: Context
    lateinit var mAdView : AdView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_route_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mRecyclerView = view!!.routeListView
        val mLayoutManager = LinearLayoutManager(context)
        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.adapter = mAdapter
        toggleStops()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getRouteObjectsFromJsonArray(to_swords)
        mAdView = Helpers.adHelper(view, requireContext())
    }

    fun getRouteObjectsFromJsonArray(to_swords: Boolean) {
        val gsonBuilder = GsonBuilder().serializeNulls()
        gsonBuilder.registerTypeAdapter(TimetableRouteTitle::class.java,
            TimetableRouteListDeserializer()
        )
        val gson = gsonBuilder.create()

        val timetableRouteList = gson.fromJson(resources.openRawResource(R.raw.timetable_stops)
            .bufferedReader().use { it.readText() }, Array<TimetableRouteTitle>::class.java)
            .toList()


        listOfStopsToSwords = timetableRouteList.get(0).value
        listOfStopsToSwords.sort()

        listOfStopsToCity = timetableRouteList.get(1).value
        listOfStopsToCity.sort()

        if(to_swords)
            mAdapter.addAll(listOfStopsToSwords)
        else
            mAdapter.addAll(listOfStopsToCity)
    }

    fun toggleStops() {
        val radioGroup: RadioGroup = route_direction_group
        val swordsRadioButton: RadioButton = route_rb_swords
        val cityRadioButton: RadioButton = route_rb_city

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when {
                (cityRadioButton.isChecked) -> {
                    to_swords = false
                    mAdapter.setDirection(to_swords)
                    listOfStopsToCity.clear()
                    getRouteObjectsFromJsonArray(to_swords)
                    mAdapter.notifyDataSetChanged()
                    mAdapter.addAll(listOfStopsToCity)

                }
                (swordsRadioButton.isChecked) -> {
                    to_swords = true
                    mAdapter.setDirection(to_swords)
                    listOfStopsToSwords.clear()
                    getRouteObjectsFromJsonArray(to_swords)
                    mAdapter.notifyDataSetChanged()
                    mAdapter.addAll(listOfStopsToSwords)
                }
            }
        }
    }
}
