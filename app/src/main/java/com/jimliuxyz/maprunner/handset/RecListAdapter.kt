package com.jimliuxyz.maprunner.handset

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.jimliuxyz.maprunner.handset.db.RunRec

class RecListAdapter: RecyclerView.Adapter<RecListAdapter.ViewHolder>() {

    private var list: List<RunRec>? = null
    private var mListener: ItemClickListener? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        var name: TextView
        var rec: RunRec? = null

        init {
            name = itemView.findViewById(android.R.id.text1)
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            mListener?.onItemClick(rec!!)
        }

        fun bind(rec: RunRec) {
            this.rec = rec
            val connected = if (BtClient.getClient().isRecConnected(rec)) " 行進中..." else ""
            name.setText(rec.title + connected)
        }
    }

    fun updateList(newlist: List<RunRec>) {
        list = newlist
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mContext = parent.context
        return ViewHolder(LayoutInflater.from(mContext)
                .inflate(android.R.layout.simple_list_item_2, parent, false))
    }

    override fun getItemCount(): Int {
        return list?.let { it.size } ?: 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        list?.get(position)?.let {
            holder.bind(it)
        }
    }


    interface ItemClickListener {
        fun onItemClick(rec: RunRec)
    }

    fun regItemClickListener(listener: ItemClickListener) {
        mListener = listener
    }

    fun unregItemClickListener() {
        mListener = null
    }
}