package com.example.covidproximity.adapters

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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

class HybridContactAdapter(val db : SQLiteDatabase) :
    RecyclerView.Adapter<HybridContactAdapter.EntryHolder>(),
    Observer {

    private var dbService : ContactDBService? = null
    private val summaries = mutableListOf<ContactSummaryModel.ContactSummary>()
    private val history = mutableListOf<ContactModel.Contact>()
    private var detailMode = false
    private lateinit var recycler : RecyclerView
    private val historyFragment by lazy {
        FragmentManager.findFragment<HistoryFragment>(recycler)
    }
    private val afterBind = LinkedList<() -> Unit>()
    private val handler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        Log.v(Const.TAG, "ContactSummaryAdapter::onAttachedRecyclerView")
        recycler = recyclerView
        with(recyclerView.context.applicationContext) {
            Intent(
                this,
                ContactDBService::class.java
            ).also {
                bindService(it, connection, android.app.Service.BIND_AUTO_CREATE)
                afterBind.add {
                    dbService?.resultDispenser?.addObserver(this@HybridContactAdapter)
                    refresh()
                }
            }
        }
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) :EntryHolder  {
//        Log.v(Const.TAG, "ContactSummaryAdapter::onCreateViewHolder")
        val frame = if (detailMode) {
            LayoutInflater.from(parent.context).inflate(R.layout.litem_contact_record, parent, false) as ViewGroup
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_contact_summary, parent, false) as ViewGroup
        }
        return EntryHolder(frame)
    }

    override fun getItemCount(): Int {
        return if (detailMode) {
            history.size
        } else {
            summaries.size
        }
    }

    override fun onBindViewHolder(holder: EntryHolder, position: Int) {
        val frame = holder.itemView as ViewGroup
        if (detailMode) {
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
                it.text = String.format("%4.1f", contactSummary.closestDistance)
            }
            frame.findViewById<TextView>(R.id.text_distance_sd).also {
                it.text = String.format("%4.1f", contactSummary.averageDistance)

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
            if (detailMode) {
                history.addAll(result)
            } else {
                summaries.addAll(ContactSummaryModel.distinctList(result))
            }
            notifyDataSetChanged()
        }
        if (o is HistoryFragment.modeNotifyer) {
            handler.post {
                detailMode = historyFragment.isDetailMode()
                history.clear()
                summaries.clear()
                refresh()
            }
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