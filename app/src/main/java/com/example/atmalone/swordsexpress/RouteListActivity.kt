package com.example.atmalone.swordsexpress


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.activity_route_list.view.*

class RouteListActivity : Fragment() {

    private var mAdapter: TimetableAdapter = TimetableAdapter()
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var listOfStopsToSwords : MutableList<String>
    private lateinit var listOfStopsToCity: MutableList<String>
    var to_swords: Boolean = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_route_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mRecyclerView = view!!.timetableListView
        val mLayoutManager = LinearLayoutManager(context)
        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.adapter = mAdapter
        toggleStops()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getRouteObjectsFromJsonArray(to_swords)
    }

    fun getRouteObjectsFromJsonArray(to_swords: Boolean) {
        val gsonBuilder = GsonBuilder().serializeNulls()
        gsonBuilder.registerTypeAdapter(TimetableRoute::class.java, TimetableRouteListDeserializer())
        val gson = gsonBuilder.create()

        val timetableRouteList = gson.fromJson(resources.openRawResource(R.raw.timetable_stops)
            .bufferedReader().use { it.readText() }, Array<TimetableRoute>::class.java).toList()

        listOfStopsToSwords = timetableRouteList.get(0).value
        listOfStopsToCity = timetableRouteList.get(1).value

        if(to_swords)
            mAdapter.addAll(listOfStopsToSwords)
        else
            mAdapter.addAll(listOfStopsToCity)
    }

    fun toggleStops() {
        val radioGroup: RadioGroup = direction_group
        val swordsRadioButton: RadioButton = rb_sunday
        val cityRadioButton: RadioButton = city

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when {
                (cityRadioButton.isChecked) -> {
                    to_swords = false
                    listOfStopsToCity.clear()
                    getRouteObjectsFromJsonArray(to_swords)
                    mAdapter.notifyDataSetChanged()
                    mAdapter.addAll(listOfStopsToCity)

                }
                (swordsRadioButton.isChecked) -> {
                    to_swords = true
                    listOfStopsToSwords.clear()
                    getRouteObjectsFromJsonArray(to_swords)
                    mAdapter.notifyDataSetChanged()
                    mAdapter.addAll(listOfStopsToSwords)
                }
            }
        }
    }
}
