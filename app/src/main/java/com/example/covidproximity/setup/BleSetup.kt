package com.example.covidproximity.setup

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.covidproximity.Const
import java.util.*

/**
 * BleSetup
 *
 * Application independent Bluetooth configuration
 *
 */
object BleSetup : Observable() {

    private val adapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    val preferPrivileges = arrayOf(
        if (Build.VERSION.SDK_INT >=28) {
            Manifest.permission.ACCESS_FINE_LOCATION
        } else {
            Manifest.permission.ACCESS_COARSE_LOCATION
        }
    )
    private var btEnable = isAdapterEnabled()
    private var locationElable = false
    private var preferPrivileageFullfill = false
    private var availability = Status.UNKNOWN

    enum class Status(val state :String) {
        OK("ok"),
        NO_ADAPTER("system has no adapter"),
        ADAPTER_IS_OFF("bluetooth is currently off"),
        NO_BLE_FUTURE("system has no BLE future"),
        NEED_PRIVILAGE("some previlage is needed to run"),
        LOCATION_IS_OFF("location is currently off"),
        UNKNOWN("not initialized yet")
    }

    fun isAdapterEnabled() : Boolean {
        adapter?.run {
            return isEnabled
        }
        return false
    }

    fun isPrivilageFullfill() :Boolean {
        return preferPrivileageFullfill
    }

    fun isLocationEnable() : Boolean {
        return locationElable
    }

    override fun setChanged() {
        val newState = getState()
        if (newState != availability) {
            super.setChanged()
        }
        availability = newState
    }

    fun onPrevileageChanged(context : Context) {
        var newPrevileageState = true
        for (previleage in preferPrivileges) {
            if (ContextCompat.checkSelfPermission(context, previleage) != PackageManager.PERMISSION_GRANTED) {
                newPrevileageState = false
            }
        }
        if (preferPrivileageFullfill != newPrevileageState) {
            preferPrivileageFullfill = newPrevileageState
            setChanged()
            notifyObservers(availability)
        }
    }

    fun onLocationChanged(context : Context) {
        var newLocationState = true
        (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).run {
            if (Build.VERSION.SDK_INT >= 28) {
                if (!this.isLocationEnabled) {
                    newLocationState = false
                }
                // TODO handle location on Lower than SDK 28
            }
        }
        if (locationElable != newLocationState) {
            locationElable = newLocationState
            setChanged()
            notifyObservers(availability)
        }
    }

    fun getState() :Status {
        return when {
            adapter == null -> Status.NO_ADAPTER
            !btEnable -> Status.ADAPTER_IS_OFF
            !preferPrivileageFullfill -> Status.NEED_PRIVILAGE
            !locationElable -> Status.LOCATION_IS_OFF
            else -> Status.OK
        }
    }

    val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.v(Const.TAG, "BleService::onReceive")
            intent?.run {
                when (this.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_ON -> {
                        btEnable = true
                        setChanged()
                        notifyObservers(availability)
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        btEnable = false
                        setChanged()
                        notifyObservers(availability)
                    }
                }
            }
        }
    }

    interface deviceChooser {
        fun found(device :BluetoothDevice)
    }
    interface deviceSelector {
        fun detected(device :BluetoothDevice)
    }
}