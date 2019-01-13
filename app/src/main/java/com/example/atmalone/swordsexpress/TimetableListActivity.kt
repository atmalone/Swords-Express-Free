package com.example.atmalone.swordsexpress

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_timetable_list.*

class TimetableListActivity : AppCompatActivity() {
//    override fun getIntent(): Intent {
//        val intentTitleExtra:String = intent.getStringExtra(STOP_TITLE)
//        val intentDirectionExtra:Boolean = intent.getBooleanExtra(DIRECTION, false)
//        return super.getIntent()
//    }
//
//    override fun <T : ExtraData?> getExtraData(extraDataClass: Class<T>?): T {
//        val intentTitleExtra:String = intent.getStringExtra(STOP_TITLE)
//        val intentDirectionExtra:Boolean = intent.getBooleanExtra(DIRECTION, false)
//        return super.getExtraData(extraDataClass)
//    }

    private var mAdapter: TimetableListAdapter = TimetableListAdapter(this)
    private lateinit var mRecyclerView: RecyclerView
    private var listOfTimetables = mutableListOf<TimetableItem>()
    private var weekSelection: Int = 0
    private var mStopTitle = ""
    private var mToSwords: Boolean = false

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
    }

//    override fun onCreate(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        val view = inflater.inflate(R.layout.activity_timetable_list, container, false)
//        val intentTitleExtra:String = activity?.intent?.getStringExtra(STOP_TITLE) ?: "Abbeyvale"
//        val intentDirectionExtra:Boolean = activity?.intent?.getBooleanExtra(DIRECTION, false) ?: false
//        mStopTitle = intentTitleExtra
//        mToSwords = intentDirectionExtra
//        return view
//    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        toggleWeek()
//        getTimetableObjectsFromJsonArray(weekSelection)
//    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        mRecyclerView = view!!.timetableListView
//        val mLayoutManager = LinearLayoutManager(context)
//        mRecyclerView.layoutManager = mLayoutManager
//        mRecyclerView.adapter = mAdapter
//    }

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
            listOfTimetables?.addAll(timetable.values)
            mAdapter.addAll(timetable.values)
        }


//        when {
//            (weekSelection == 0) -> {
//                mAdapter.addAll(listOfWeekdayTimetables)
//            }
//            (weekSelection == 1) -> {
//                mAdapter.addAll(listOfSaturdayTimetables)
//            }
//            (weekSelection == 2) -> {
//                mAdapter.addAll(listOfSundayTimetables)
//            }
//        }
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
