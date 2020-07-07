package com.example.covidproximity.adapters

import android.content.ComponentName
import android.content.ServiceConnection
import android.database.sqlite.SQLiteDatabase
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.covidproximity.Const
import com.example.covidproximity.R
import com.example.covidproximity.models.ContactSummaryModel
import com.example.covidproximity.models.ContactModel
import com.example.covidproximity.tasks.ContactDBService
import com.example.covidproximity.ui.HistoryFragment
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ContactSummaryAdapter(val db : SQLiteDatabase) :
    RecyclerView.Adapter<ContactSummaryAdapter.EntryHolder>(),
    Observer {

    private var dbService : ContactDBService? = null
    val summaries = mutableListOf<ContactSummaryModel.ContactSummary>()
    val history = mutableListOf<ContactModel.Contact>()
    var summaryMode = false
    private lateinit var recycler : RecyclerView
    val historyFragment by lazy {
        FragmentManager.findFragment<HistoryFragment>(recycler)
    }
    private val afterBind = LinkedList<() -> Unit>()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        Log.v(Const.TAG, "ContactSummaryAdapter::onAttachedRecyclerView")
        recycler = recyclerView
        with(recyclerView.context.applicationContext) {
            android.content.Intent(
                this,
                com.example.covidproximity.tasks.ContactDBService::class.java
            ).also {
                bindService(it, connection, android.app.Service.BIND_AUTO_CREATE)
                afterBind.add {
                    dbService?.resultDispenser?.addObserver(this@ContactSummaryAdapter)
                    refresh()
                }
            }
        }
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) :EntryHolder  {
//        Log.v(Const.TAG, "ContactSummaryAdapter::onCreateViewHolder")
        val frame = if (summaryMode) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_contact_summary, parent, false) as ViewGroup
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.litem_contact_record, parent, false) as ViewGroup
        }
        return EntryHolder(frame)
    }

    override fun getItemCount(): Int {
        return if (summaryMode) {
            summaries.size
        } else {
            history.size
        }
    }

    override fun onBindViewHolder(holder: EntryHolder, position: Int) {
        val frame = holder.itemView as ViewGroup
        if (summaryMode) {
            val contactSummary = summaries.get(position)
            frame.findViewById<TextView>(R.id.text_datetime_begin).also {
                it.text = mdhmsFormatter.format(contactSummary.since)
            }
            frame.findViewById<TextView>(R.id.text_datetime_end).also {
                it.text = hmsFormatter.format(contactSummary.till)
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
            frame.findViewById<TextView>(R.id.text_key).also {
                it.text = contactSummary.key.toString()
            }
        } else {
            frame.findViewById<TextView>(R.id.view_history_time).also {
                it.text = history.get(position).date.toString()
            }
            frame.findViewById<TextView>(R.id.view_history_key).also {
                it.text = history.get(position).uuid.toString()
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Log.v(Const.TAG, "ContactSummaryAdapter::onDetachedFromRecyclerView")
        dbService?.resultDispenser?.deleteObserver(this)
        recyclerView.context.applicationContext.unbindService(connection)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o is ContactDBService.ResultDispenser) {
            val result = arg as List<ContactModel.Contact>
            if (summaryMode) {
                summaries.addAll(ContactSummaryModel.distinctList(result))
            } else {
                history.addAll(result)
            }
            notifyDataSetChanged()
        }
        if (o is HistoryFragment.modeNotifyer) {
            summaryMode = !historyFragment.isDetailMode()
            history.clear()
            summaries.clear()
            refresh()
        }
    }

    private fun refresh() {
        when (historyFragment.getDuration()) {
            HistoryFragment.InstrumentDuration.TODAY -> {
                dbService?.access { db -> ContactModel.getToday(db) }
            }
            HistoryFragment.InstrumentDuration.ONE_WEEK -> {
                dbService?.access { db -> ContactModel.getOneWeek(db) }
            }
            HistoryFragment.InstrumentDuration.TWO_WEEKS -> {
                dbService?.access { db -> ContactModel.getTwoWeeks(db) }
            }
            else -> throw Exception("oops")
        }
    }

    class EntryHolder(itemView: ViewGroup) : RecyclerView.ViewHolder(itemView)

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
    val mdhmsFormatter = SimpleDateFormat("m d h:mm:ss")
    val hmsFormatter = SimpleDateFormat("h:mm:ss")
}