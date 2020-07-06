package com.example.covidproximity.adapters

import android.content.ComponentName
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
import com.example.covidproximity.models.ContactSummaryModel
import com.example.covidproximity.models.ContactModel
import com.example.covidproximity.tasks.ContactDBService
import java.util.*

class ContactSummaryAdapter(val db : SQLiteDatabase) :
    RecyclerView.Adapter<ContactSummaryAdapter.SummaryHolder>(),
    Observer {

    private var dbService : ContactDBService? = null
    val summaries = mutableListOf<ContactSummaryModel.ContactSummary>()
    private val afterBind = LinkedList<() -> Unit>()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        Log.v(Const.TAG, "ContactSummaryAdapter::onAttachedRecyclerView")
        with(recyclerView.context.applicationContext) {
            android.content.Intent(
                this,
                com.example.covidproximity.tasks.ContactDBService::class.java
            ).also {
                bindService(it, connection, android.app.Service.BIND_AUTO_CREATE)
                afterBind.add {
                    dbService?.resultDispenser?.addObserver(this@ContactSummaryAdapter)
                    dbService?.access{ db -> ContactSummaryModel.getTodaysSummary(db)}
                }
            }
        }
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : SummaryHolder {
//        Log.v(Const.TAG, "ContactSummaryAdapter::onCreateViewHolder")
        val frame = LayoutInflater.from(parent.context).inflate(R.layout.item_contact_summary, parent, false) as ViewGroup
        return SummaryHolder(frame)
    }

    override fun getItemCount(): Int {
        return summaries.size
    }

    override fun onBindViewHolder(holder: SummaryHolder, position: Int) {
        val frame = holder.itemView as ViewGroup
        val contactSummary = summaries.get(position)
        frame.findViewById<TextView>(R.id.text_datetime_begin).also {
            it.text = contactSummary.since.toString()
        }
        frame.findViewById<TextView>(R.id.text_datetime_end).also {
            it.text = contactSummary.till.toString()
        }
        frame.findViewById<TextView>(R.id.text_occureance).also {
            it.text = contactSummary.count.toString()
        }
        frame.findViewById<TextView>(R.id.text_distance_min).also {
            it.text = contactSummary.closestDistance.toString()
        }
        frame.findViewById<TextView>(R.id.text_distance_sd).also {
            it.text = contactSummary.averageDistance.toString()
        }
        frame.findViewById<TextView>(R.id.text_key). also {
            it.text = contactSummary.key.toString()
        }
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o is ContactDBService.ResultDispenser) {
            val result = arg as List<ContactSummaryModel.ContactSummary>
            summaries.addAll(result)
            notifyDataSetChanged()
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Log.v(Const.TAG, "ContactSummaryAdapter::onDetachedFromRecyclerView")
        dbService?.resultDispenser?.deleteObserver(this)
        recyclerView.context.applicationContext.unbindService(connection)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    class SummaryHolder(itemView: ViewGroup) : RecyclerView.ViewHolder(itemView)

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.v(Const.TAG, "ContactSummaryAdapter::onServiceDisconnected")
            dbService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.v(Const.TAG, "ContactSummaryAdapter::onServiceConnected")
            val binder = service as ContactDBService.LocalBinder
            dbService = binder.getService()
            afterBind.forEach{it.invoke()}
        }
    }
}