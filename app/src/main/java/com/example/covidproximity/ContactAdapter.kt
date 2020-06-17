package com.example.covidproximity

import android.database.sqlite.SQLiteDatabase
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(val db : SQLiteDatabase) : RecyclerView.Adapter<ContactAdapter.HistoryHolder>() {

    val list by lazy {
        ContactHistory.getAll(db)
    }
    class HistoryHolder(itemView: ViewGroup) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
        val frame = LayoutInflater.from(parent.context).inflate(R.layout.layout_history_record, parent, false) as ViewGroup
        return HistoryHolder(frame)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        val frame = holder.itemView as ViewGroup
        frame.findViewById<TextView>(R.id.view_history_time).also {
            it.text = list.get(position).date.toString()
        }
        frame.findViewById<TextView>(R.id.view_history_key).also {
            it.text = list.get(position).uuid.toString()
        }
    }
}