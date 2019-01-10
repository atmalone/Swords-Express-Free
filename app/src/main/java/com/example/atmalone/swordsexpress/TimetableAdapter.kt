package com.example.atmalone.swordsexpress


import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.ArrayList

class TimetableAdapter: RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {

   private var timetableRouteList: MutableList<String> = ArrayList()

    internal fun addAll(list: MutableList<String>) {
        timetableRouteList = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemView = layoutInflater.inflate(R.layout.route_list_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount() = timetableRouteList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTitleView.text  = timetableRouteList[position]
    }

    class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView){
        val mTitleView: TextView = mView.findViewById(R.id.txt_route)

    }
}