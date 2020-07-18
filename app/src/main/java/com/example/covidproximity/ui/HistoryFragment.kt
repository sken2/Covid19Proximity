package com.example.covidproximity.ui

import android.os.*
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.covidproximity.Const
import com.example.covidproximity.MainActivity
import com.example.covidproximity.R
import com.example.covidproximity.adapters.HybridContactAdapter
import com.example.covidproximity.adapters.HistoryDBWrapper
import java.util.*

class HistoryFragment : Fragment() {

    lateinit var manager : RecyclerView.LayoutManager
    private val mainActivity by lazy {
        activity as MainActivity
    }
    private var detailMode = false
    private var duration = InstrumentDuration.TODAY
    private var actionMode : androidx.appcompat.view.ActionMode? = null
    private val recyclerView by lazy {
        view?.findViewById<RecyclerView>(R.id.recycler_history)
    }
    private val handler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        restoreState(savedInstanceState)
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
        restoreState(savedInstanceState)

        view.findViewById<Button>(R.id.button_second).setOnClickListener {
            findNavController().navigate(R.id.action_historyFragment_to_controlFragment)
        }
        manager = LinearLayoutManager(view.context).apply {
            canScrollVertically()
        }
        recyclerView?.apply {
            setHasFixedSize(true)
            layoutManager = manager
            val viewAdapter = HybridContactAdapter(HistoryDBWrapper(context).readableDatabase).also {
                modeNotifyer.addObserver(it)
            }
            adapter = viewAdapter
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.v(Const.TAG, "HistoryFragment::onSaveInstanceState")
        saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        Log.v(Const.TAG, "HistoryFragment::onPause")
        super.onPause()
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
                detailMode = false
                manager.removeAllViews()
                modeNotifyer.changed()
                return true
            }
            R.id.menu_detail -> {
                detailMode = true
                manager.removeAllViews()
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

    fun restoreState(savedInstanceState: Bundle?) {
        savedInstanceState?.run {
            detailMode = getBoolean(Const.Keys.DetailMode)
            getString(Const.Keys.Duration)?.let {
                duration = InstrumentDuration.valueOf(it)
            }
        }
    }

    fun saveState(bundle : Bundle) {
        bundle.putBoolean(Const.Keys.DetailMode, detailMode)
        bundle.putString(Const.Keys.Duration, duration.name)
    }

    object modeNotifyer : Observable() {

        fun changed() {
            setChanged()
            notifyObservers()
        }
    }

    enum class InstrumentDuration() {
        TODAY(),
        ONE_WEEK(),
        TWO_WEEKS(),
        WHOLE_DATA()
    }
}
