package com.example.covidproximity.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.covidproximity.Const
import com.example.covidproximity.R
import com.example.covidproximity.setup.BleSetup

class SetupFlagment : Fragment() {

    val btButton by lazy {
        view?.findViewById<Button>(R.id.button_bluetooth_change)
    }
    val btDescription by lazy {
        view?.findViewById<TextView>(R.id.text_bluetooth_description)
    }
    val privilageButton by lazy {
        view?.findViewById<Button>(R.id.button_privileage_change)
    }
    val privilageDescription by lazy {
        view?.findViewById<TextView>(R.id.text_privilage_description)
    }
    val locationButton by lazy {
        view?.findViewById<Button>(R.id.button_location_change)
    }
    val locationDescription by lazy {
        view?.findViewById<TextView>(R.id.text_location_description)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(Const.TAG, "SetupFragment::onViewCreated")
        with(view) {
            findViewById<Switch>(R.id.switch_recall_bluetooth).apply {
                setOnCheckedChangeListener { buttonView, isChecked ->
                    //set preference
                }
            }
            findViewById<Switch>(R.id.switch_recall_privilage).apply {
                setOnCheckedChangeListener{ buttonView, isChecked ->

                }
            }
            findViewById<Switch>(R.id.switch_recall_location).apply {
                setOnCheckedChangeListener{ buttonView, isChecked ->

                }
            }
        }
        btButton?.setOnClickListener {
            Toast.makeText(context, "Bluetooth turn to on", Toast.LENGTH_SHORT).show()  //TODO
        }
        privilageButton?.setOnClickListener {
            Toast.makeText(context, "Privilege turn to on", Toast.LENGTH_SHORT).show()  //TODO
        }
        locationButton?.setOnClickListener {
            Toast.makeText(context, "Bluetooth turn to on", Toast.LENGTH_SHORT).show()  //TODO
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        with(view) {
            BleSetup.isAdapterEnabled().let {
                btButton?.isEnabled = !it
                btDescription?.text = "Bluetooth is ${offon(it)}"
            }
            BleSetup.isPrivilageFullfill().let {
                privilageButton?.isEnabled = !it
                privilageDescription?.text = "Access permission is ${offon(it)}"
            }
            BleSetup.isLocationEnable().let {
                locationButton?.isEnabled = !it
                locationDescription?.text = "Location provider is ${offon(it)}"
            }
        }
    }

    private fun offon(state : Boolean) : String {
        return if (state) {
            "On"
        } else {
            "Off"
        }
    }
}