package com.example.covidproximity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.util.*

class SetupFlagment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.button_to_control)?.apply {
            this.setOnClickListener {
                findNavController().navigate(R.id.action_to_control_from_setup)
            }
        }
        view.findViewById<Button>(R.id.button_to_contact)?.apply {
            this.setOnClickListener {
                findNavController().navigate(R.id.action_to_history_from_setup)
            }
        }

        view.findViewById<Button>(R.id.button_to_aboutthis)?.apply {
            setOnClickListener{
                val wdb = ContactHistory.HistoryDB(view.context).writableDatabase
                ContactHistory.record(wdb, UUID.randomUUID())
                Toast.makeText(view.context, "data inserted", Toast.LENGTH_SHORT).show()
                wdb.close()
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }
}