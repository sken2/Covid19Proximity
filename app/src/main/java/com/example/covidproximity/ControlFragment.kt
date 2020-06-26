package com.example.covidproximity

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import androidx.navigation.fragment.findNavController
import java.util.*

class ControlFragment : Fragment(), Observer {

    val advertiseSwitch by lazy {
        view?.findViewById<Switch>(R.id.switch_advertise)
    }
    val scanSwitch by lazy {
        view?.findViewById<Switch>(R.id.switch_scan)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.v(Const.TAG, "ControlFragment::onCreateView")
        return inflater.inflate(R.layout.fragment_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(Const.TAG, "ControlFragment::onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            advertiseSwitch?.apply {
                this.isChecked = Corona.isAdvertising()
                this.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        Corona.startAdvertising()
                    } else {
                        Corona.stopAdvertising()
                    }
                }
            }
            scanSwitch?.apply {
                this.isChecked = Corona.isScanning()
                this.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        Corona.startScanning()
                    } else {
                        Corona.stopScanning()
                    }
                }
            }
            findViewById<Button>(R.id.button_show_contact).setOnClickListener {
                findNavController().navigate(R.id.action_controlFragment_to_historyFragment)
            }
            findViewById<Button>(R.id.button_system_setup).setOnClickListener {
                findNavController().navigate(R.id.action_controlFragment_to_setupFlagment)
            }
            findViewById<Button>(R.id.button_stop_service).setOnClickListener{
                if (it.context is MainActivity) {
                    val activity = it.context as MainActivity
                    activity.bleService?.shutdown()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Corona.addObserver(this)
        advertiseSwitch?.isChecked = Corona.isAdvertising()
        scanSwitch?.isChecked = Corona.isScanning()
    }

    override fun onPause() {
        Corona.deleteObserver(this)
        super.onPause()
    }

    override fun onDestroyView() {
        Log.v(Const.TAG, "ControlFragment::onDestroyView")
        super.onDestroyView()
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o is Corona) {
            advertiseSwitch?.isChecked = o.isAdvertising()
            scanSwitch?.isChecked = o.isScanning()
        }
    }
}
