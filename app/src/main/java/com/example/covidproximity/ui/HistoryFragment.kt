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
import com.example.covidproximity.adapters.ContactAdapter
import com.example.covidproximity.R
import com.example.covidproximity.adapters.HistoryDBWrapper

class HistoryFragment : Fragment() {

    lateinit var manager : RecyclerView.LayoutManager

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
        view.findViewById<RecyclerView>(R.id.recycler_history).apply {
            setHasFixedSize(true)
            layoutManager = manager
            adapter = ContactAdapter(
                HistoryDBWrapper(view.context).readableDatabase
            )
        }
    }

    override fun onDestroyView() {
        Log.v(Const.TAG, "HistoryFragment::onDestroyView")
        super.onDestroyView()
    }

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.option_menu, menu)
//        return
//    }
}