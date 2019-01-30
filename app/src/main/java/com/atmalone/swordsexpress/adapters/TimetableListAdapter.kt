package com.atmalone.swordsexpress.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.atmalone.swordsexpress.models.TimetableItem
import com.atmalone.swordsexpress.R
import java.util.ArrayList

class TimetableListAdapter(context: Context?) : RecyclerView.Adapter<TimetableListAdapter.ViewHolder>() {

    private var timetableList: MutableList<TimetableItem> = ArrayList()
    private var timetableRouteTitle: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemView = layoutInflater.inflate(R.layout.info_list_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount() = timetableList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTitleView.text = timetableList[position].title
        holder.mRouteView.text = timetableList[position].route
        holder.mTimeView.text = timetableList[position].time
    }

    internal fun addAll(list: MutableList<TimetableItem>) {
        timetableList = list
    }

    internal fun add(timetable: TimetableItem) {
        timetableList.add(timetable)
    }

    class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView){
        val mTitleView: TextView = mView.findViewById(R.id.txt_route)
        val mTimeView: TextView = mView.findViewById(R.id.txt_time)
        val mRouteView: TextView = mView.findViewById(R.id.txt_stop)
    }
}