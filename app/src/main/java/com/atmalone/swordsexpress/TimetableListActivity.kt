package com.atmalone.swordsexpress

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_timetable_list.*
import com.google.android.gms.ads.AdListener



class TimetableListActivity : AppCompatActivity() {
    private var mAdapter: TimetableListAdapter = TimetableListAdapter(this)
    private lateinit var mRecyclerView: RecyclerView
    private var listOfTimetables = mutableListOf<TimetableItem>()
    private var weekSelection: Int = 0
    private var mStopTitle = ""
    private var mToSwords: Boolean = false
    lateinit var mAdView : AdView
    private lateinit var mInterstitialAd: InterstitialAd


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timetable_list)
        intent
        mStopTitle = intent.getStringExtra(STOP_TITLE)
        mToSwords = intent.getBooleanExtra(DIRECTION, false)
        mRecyclerView = timetableListView
        val mLayoutManager = LinearLayoutManager(this)
        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.adapter = mAdapter
        getTimetableObjectsFromJsonArray(weekSelection)
        toggleWeek()
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-1735218136968931/1940556846"
        mInterstitialAd.loadAd(AdRequest.Builder().build())
    }

    override fun onStart() {
        super.onStart()
        mAdView = Helpers.adHelper(findViewById(R.id.adView), this)

        mInterstitialAd.adListener = object : AdListener() {
            override fun onAdLoaded() {
                mInterstitialAd.show()
            }
        }

    }

    fun getTimetableObjectsFromJsonArray(weekSelection: Int) {
        val gsonBuilder = GsonBuilder().serializeNulls()
        gsonBuilder.registerTypeAdapter(Timetable::class.java, TimetableDeserializer())
        val gson = gsonBuilder.create()
        var resource = selectRawResource(mToSwords, weekSelection)
        val timetableRouteList = gson.fromJson(resources.openRawResource(resource)
                .bufferedReader().use { it.readText() }, Array<Timetable>::class.java)
                .toList()

        var timetable = timetableRouteList.find { it.title == mStopTitle }

        if (timetable != null) {
            listOfTimetables.addAll(timetable.values)
            mAdapter.addAll(timetable.values)
        }
    }

    fun toggleWeek() {
        val radioGroup: RadioGroup = week_group
        val weekdayRadioButton: RadioButton = rb_weekday
        val saturdayRadioButton: RadioButton = rb_saturday
        val sundayRadioButton: RadioButton = rb_sunday

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when {
                (weekdayRadioButton.isChecked) -> {
                    weekSelection = 0
                    listOfTimetables.clear()
                    getTimetableObjectsFromJsonArray(0)
                    mAdapter.notifyDataSetChanged()
                    mAdapter.addAll(listOfTimetables)

                }
                (saturdayRadioButton.isChecked) -> {
                    weekSelection = 1
                    listOfTimetables.clear()
                    getTimetableObjectsFromJsonArray(1)
                    mAdapter.notifyDataSetChanged()
                    mAdapter.addAll(listOfTimetables)
                }  
                (sundayRadioButton.isChecked) -> {
                    weekSelection = 2
                    listOfTimetables.clear()
                    getTimetableObjectsFromJsonArray(2)
                    mAdapter.notifyDataSetChanged()
                    mAdapter.addAll(listOfTimetables)
                }
            }
        }
    }
    
    fun selectRawResource(mToSwords: Boolean, weekSelection: Int) : Int {
        var weekSelectionRawResource = R.raw.city_swords_mon_fri

        when {
            (mToSwords && weekSelection == 0) -> {
                weekSelectionRawResource = R.raw.city_swords_mon_fri
            }
            (mToSwords && weekSelection == 1) -> {
                weekSelectionRawResource = R.raw.city_swords_sat
            }
            (mToSwords && weekSelection == 2) -> {
                weekSelectionRawResource = R.raw.city_swords_sun
            }
            (!mToSwords && weekSelection == 0) -> {
                weekSelectionRawResource = R.raw.swords_city_mon_fri
            }
            (!mToSwords && weekSelection == 1) -> {
                weekSelectionRawResource = R.raw.swords_city_sat
            }
            (!mToSwords && weekSelection == 2) -> {
                weekSelectionRawResource = R.raw.swords_city_sun
            }
        }
        return weekSelectionRawResource
    }
}
