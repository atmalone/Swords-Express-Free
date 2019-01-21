package com.atmalone.swordsexpress


import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.*

class RouteListAdapter() : RecyclerView.Adapter<RouteListAdapter.ViewHolder>() {

    private var routeList: MutableList<String> = ArrayList()
    private lateinit var mContext: Context
    private var mDirection = false

    internal fun addAll(list: MutableList<String>) {
        routeList = list
    }

    internal fun setDirection(direction: Boolean) {
        mDirection = direction
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemView = layoutInflater.inflate(R.layout.route_list_item, parent, false)
        mContext = parent.context
        return ViewHolder(itemView)
    }

    override fun getItemCount() = routeList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTitleView.text  = routeList[position]
    }

    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView){
        val mTitleView: TextView = mView.findViewById(R.id.txt_route)
        init {
            mView.setOnClickListener {
                val intent = Intent(mContext, TimetableListActivity::class.java)
                intent.putExtra(STOP_TITLE, mTitleView.text)
                intent.putExtra(DIRECTION, mDirection)
                mContext.startActivity(intent)
            }
        }
    }
}