package com.example.covidproximity.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import androidx.navigation.fragment.findNavController
import com.example.covidproximity.Const
import com.example.covidproximity.MainActivity
import com.example.covidproximity.R
import com.example.covidproximity.entities.Covid19
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
                this.isChecked = Covid19.isAdvertising()
                this.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        this.context?.run {
                            if (context is MainActivity) {
                                val mainActivity = context as MainActivity
                                mainActivity?.bleService?.let {
                                    Covid19.KeyDispenser.start(it)
                                }
                            }
                        }
                    } else {
                        Covid19.KeyDispenser.stop()
                    }
                }
            }
            scanSwitch?.apply {
                this.isChecked = Covid19.isScanning()
                this.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        Covid19.startScanning()
                    } else {
                        Covid19.stopScanning()
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
        Covid19.addObserver(this)
        advertiseSwitch?.isChecked = Covid19.isAdvertising()
        scanSwitch?.isChecked = Covid19.isScanning()
    }

    override fun onPause() {
        Covid19.deleteObserver(this)
        super.onPause()
    }

    override fun onDestroyView() {
        Log.v(Const.TAG, "ControlFragment::onDestroyView")
        super.onDestroyView()
    }

    override fun update(o: Observable?, arg: Any?) {
        if (o is Covid19) {
            advertiseSwitch?.isChecked = o.isAdvertising()
            scanSwitch?.isChecked = o.isScanning()
        }
    }
}
