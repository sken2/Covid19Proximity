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

class ControlFragment : Fragment() {

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
        view.findViewById<Switch>(R.id.switch_advertise)?.apply {
            this.isChecked = Corona.isAdvertising()
            this.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    Corona.startAdvertising()
                } else {
                    Corona.stopAdvertising()
                }
            }
        }
        view.findViewById<Switch>(R.id.switch_scan)?.apply {
            this.isChecked = Corona.isScanning()
            this.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    Corona.startAdvertising()
                } else {
                    Corona.stopAdvertising()
                }
            }
        }
        view.findViewById<Button>(R.id.button_show_contact).setOnClickListener {
            findNavController().navigate(R.id.action_to_setup_from_control)
        }
        view.findViewById<Button>(R.id.button_stop_service).setOnClickListener{
            if (it.context is MainActivity) {
                val activity = it.context as MainActivity
                activity.bleService?.shutdown()
            }
        }
    }

    override fun onDestroyView() {
        Log.v(Const.TAG, "ControlFragment::onDestroyView")
        super.onDestroyView()
    }
}
