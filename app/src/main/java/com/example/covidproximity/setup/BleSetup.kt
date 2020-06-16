package com.example.covidproximity.setup

import android.Manifest
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.covidproximity.*
import java.util.*

object BleSetup : Observable(){

    val adapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    val preferPlivileges = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private var btEnable = false
    private var availability = Status.UNKNOWN

    enum class Status(val state :String) {
        OK("ok"),
        NO_ADAPTER("system has no adapter"),
        ADAPTER_IS_OFF("bluetooth is currently off"),
        NO_BLE_FUTURE("system has no BLE future"),
        NEED_PRIVILAGE("some previlage is needed to run"),
        LOCATION_IS_OFF("location is currently off"),
        UNKNOWN("unknown")
    }

    fun getState(context: Context) : Status {
        if (adapter == null) {
            return Status.NO_ADAPTER
        }
        if (!adapter!!.isEnabled) {
            return Status.ADAPTER_IS_OFF
        }
        for (previleage in preferPlivileges) {
            if (ContextCompat.checkSelfPermission(context, previleage) != PackageManager.PERMISSION_GRANTED) {
                return Status.NEED_PRIVILAGE
            }
        }
        (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).run {
            if (Build.VERSION.SDK_INT >= 28) {
                if (!this.isLocationEnabled) {
                    return Status.LOCATION_IS_OFF
                }
            }
        }
        return Status.OK
    }

    fun assert(context : Context) : Boolean{
        checkNotNull(adapter) {
            return false
        }
        adapter?.apply {
            if (!this.isEnabled) {
                return false
            }
        }
        for (previleage in preferPlivileges) {
            if (ContextCompat.checkSelfPermission(context, previleage) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).run {
            if (Build.VERSION.SDK_INT >= 28) {
                if (!this.isLocationEnabled) {
                    return false
                }
            }
        }
        return true
    }

    fun ask(context : Context) {
        var lackingPrivilege = emptyArray<String>()
        for (previlage in preferPlivileges) {
            if (ContextCompat.checkSelfPermission(context, previlage) != PackageManager.PERMISSION_GRANTED) {
                lackingPrivilege += previlage
            }
        }
        val locationEnabled =
            if (Build.VERSION.SDK_INT >= 28) {
                (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).run {
                    this.isLocationEnabled
                }
            } else {
                true    // TODO get LocationManager status in OLD version
            }
        when {
            context is Activity -> {
                val activity = context as Activity
                adapter?.run {
                    if (!this.isEnabled) {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        activity.startActivityForResult(enableBtIntent,
                            Const.Resuest.REQUEST_ENABLE_BT
                        )
                    }
                }
                if (lackingPrivilege.isNotEmpty()) {
                    activity.requestPermissions(lackingPrivilege,
                        Const.Resuest.REQUEST_PREVILEAGES
                    )
                }
                if (!locationEnabled) {
                    // TODO create dialog
                    val enableLocationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    activity.startActivityForResult(enableLocationIntent,
                        Const.Resuest.REQUEST_ENABLE_LOCATION
                    )
                }
            }
            context is Service -> {
                val service = context as Service
                val builder = if (Build.VERSION.SDK_INT >= 26) {
                    Notification.Builder(service, service.getString(R.string.not_channel_id))
                } else {
                    Notification.Builder(service)
                }
                when (false) {
                    adapter?.isEnabled -> {
                        builder.setSmallIcon(R.drawable.ic_proximity)
                        builder.setTicker("enable adapter")
                        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        val pendingIntent = PendingIntent.getActivity(service, 0, intent, 0)
                        builder.setContentIntent(pendingIntent)
                    }
                    lackingPrivilege.isEmpty() -> {
                        builder.setSmallIcon(R.drawable.ic_proximity)
                        builder.setTicker("check permissions")
                        val intent = Intent(service, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(service, 0, intent, 0)
                        builder.setContentIntent(pendingIntent)
                    }
                    locationEnabled -> {
                        builder.setSmallIcon(R.drawable.ic_proximity)
                        builder.setTicker("enable location")
                        val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                        val pendingIntent = PendingIntent.getActivity(service, 0, intent, 0)
                        builder.setContentIntent(pendingIntent)
                    }
                    else -> {
                        builder.setSmallIcon(R.drawable.ic_proximity)
                        builder.setTicker("Running")
                    }
                }
                val nm = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(12345, builder.build()) //TODO use proper id
            }
            else -> {
                Log.e(Const.TAG, "BleSetup::ask() unkown type of context")
            }
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
}