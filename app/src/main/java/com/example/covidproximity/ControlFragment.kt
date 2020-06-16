package com.example.covidproximity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import androidx.navigation.fragment.findNavController

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ControlFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.findViewById(R.id.switch_advertise) as Switch).apply {
            this.isChecked = Corona.isAdvertising()
            this.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    Corona.startAdvertising()
                } else {
                    Corona.stopAdvertising()
                }
            }
        }
        (view.findViewById(R.id.switch_scan) as Switch).apply {
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
    }
}
