package com.example.covidproximity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class HistoryFragment : Fragment() {

    lateinit var manager : RecyclerView.LayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
            adapter = ContactAdapter(ContactHistory.HistoryDB(view.context).readableDatabase)
        }
    }
}
