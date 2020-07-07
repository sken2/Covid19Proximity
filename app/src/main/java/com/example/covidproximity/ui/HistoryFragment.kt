package com.example.covidproximity.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.covidproximity.Const
import com.example.covidproximity.MainActivity
import com.example.covidproximity.adapters.ContactAdapter
import com.example.covidproximity.R
import com.example.covidproximity.adapters.ContactSummaryAdapter
import com.example.covidproximity.adapters.HistoryDBWrapper
import java.util.*

class HistoryFragment : Fragment() {

    lateinit var manager : RecyclerView.LayoutManager
    private val mainActivity by lazy {
        activity as MainActivity
    }
    private var detailMode = false
    private var resetAdapter = true
    private var duration = InstrumentDuration.TODAY
    private var actionMode : androidx.appcompat.view.ActionMode? = null
    private val recyclerView by lazy {
        view?.findViewById<RecyclerView>(R.id.recycler_history)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.v(Const.TAG, "HistoryFragment::onCreateView")
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(Const.TAG, "HistoryFragment::onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_second).setOnClickListener {
            findNavController().navigate(R.id.action_historyFragment_to_controlFragment)
        }
        manager = LinearLayoutManager(view.context).apply {
            canScrollVertically()
        }
        recyclerView?.apply {
            setHasFixedSize(true)
            layoutManager = manager
            attachAdapter(this)
        }
    }

    override fun onDestroyView() {
        Log.v(Const.TAG, "HistoryFragment::onDestroyView")
        modeNotifyer.deleteObservers()
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.v(Const.TAG, "HistoryFragment::onCreateOptionsMenu")
        inflater.inflate(R.menu.history_options, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_summarize -> {
                resetAdapter = detailMode
                detailMode = false
                modeNotifyer.changed()
                return true
            }
            R.id.menu_detail -> {
                resetAdapter = !detailMode
                detailMode = true
                modeNotifyer.changed()
                return true
            }
            R.id.menu_today -> {
                duration = InstrumentDuration.TODAY
                modeNotifyer.changed()
                return true
            }
            R.id.menu_an_week -> {
                duration = InstrumentDuration.ONE_WEEK
                modeNotifyer.changed()
                return true
            }
            R.id.menu_two_weeks -> {
                duration = InstrumentDuration.TWO_WEEKS
                modeNotifyer.changed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun getDuration() = duration

    fun isDetailMode() = detailMode

    private fun attachAdapter(recyclerView: RecyclerView) {
        if (resetAdapter) {
            modeNotifyer.deleteObservers()
            view?.let {
                recyclerView.adapter = if (detailMode) {
                    ContactAdapter(HistoryDBWrapper(it.context).readableDatabase)
                } else {
                    ContactSummaryAdapter(HistoryDBWrapper(it.context).readableDatabase)
                }
            }
        }
    }

    object modeNotifyer : Observable() {

        fun changed() {
            setChanged()
            notifyObservers()
        }
    }

    enum class InstrumentDuration {
        TODAY,
        ONE_WEEK,
        TWO_WEEKS,
        WHOLE_DATA
    }
}
