package com.example.covidproximity.adapters

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.database.sqlite.SQLiteDatabase
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.covidproximity.Const
import com.example.covidproximity.R
import com.example.covidproximity.models.ContactModel
import com.example.covidproximity.tasks.ContactDBService
import java.util.*

class ContactAdapter(val db : SQLiteDatabase) :
    RecyclerView.Adapter<ContactAdapter.HistoryHolder>(),
    Observer
{

    private var dbService : ContactDBService? = null
    private val list = mutableListOf<ContactModel.Contact>()
    private val afterBind = LinkedList<() -> Unit>()

    init {
        Log.v(Const.TAG, "ContactAdapter::init")
    }

    class HistoryHolder(itemView: ViewGroup) : RecyclerView.ViewHolder(itemView)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        Log.v(Const.TAG, "ContactAdapter::onAttachedToRecyclerView")
        with(recyclerView.context.applicationContext) {
            android.content.Intent(
                this,
                com.example.covidproximity.tasks.ContactDBService::class.java
            ).also {
                bindService(it, connection, android.app.Service.BIND_AUTO_CREATE)
                afterBind.add {
                    dbService?.resultDispenser?.addObserver(this@ContactAdapter)
                    dbService?.getAllHistory()
                }
            }
        }
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
        Log.v(Const.TAG, "ContactAdapter::onCreateViewHolder")
        val frame = LayoutInflater.from(parent.context).inflate(R.layout.layout_history_record, parent, false) as ViewGroup
        return HistoryHolder(
            frame
        )
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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Log.v(Const.TAG, "ContactAdapter::onDetachedFromRecyclerView")
        dbService?.resultDispenser?.deleteObserver(this)
        recyclerView.context.applicationContext.unbindService(connection)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.v(Const.TAG, "ContactAdapter::onServiceDisconnected")
            dbService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.v(Const.TAG, "ContactAdapter::onServiceConnected")
            val binder = service as ContactDBService.LocalBinder
            dbService = binder.getService()
            afterBind.forEach{it.invoke()}
        }
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o is ContactDBService.ResultDispenser) {
            val result = arg as List<ContactModel.Contact>
            list.addAll(result)
            notifyDataSetChanged()
        }
    }
}