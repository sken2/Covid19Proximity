package com.example.covidproximity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Typeface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import com.example.covidproximity.setup.BleSetup

class MainActivity : AppCompatActivity() {

    val adapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private val nm by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    val adapterFeatures : TextView? by lazy {
        findViewById(R.id.features) as TextView
    }
    val advertiserSwitch : Switch? by lazy {
        findViewById(R.id.switch_adv) as Switch
    }
    val scannerSwitch : Switch? by lazy {
        findViewById(R.id.switch_scan) as Switch
    }
    var bleService : BleService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createChannel()

        setContentView(R.layout.activity_main)

        adapterFeatures?.text = bluetoothSpec()
        if (BleSetup.assert(this)) {
            val service = Intent(this, BleService::class.java).apply {
                this.action = Const.Action.START_SENSE
            }
            startService(service)
        } else {
            BleSetup.ask(this)
        }
        advertiserSwitch?.isChecked = Corona.isAdvertising()
        advertiserSwitch?.apply {
            this.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    Corona.startAdvertising()
                } else {
                    Corona.stopAdvertising()
                }
            }
        }
        scannerSwitch?.isChecked = Corona.isScanning()
        scannerSwitch?.apply {
            this.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    Corona.startScanning()
                } else {
                    Corona.stopScanning()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        intent?.run {
            if (action == null) {
                Log.i(Const.TAG, "MainActivity::onNewIntent no action on intent")
            }
        }
        super.onNewIntent(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            Const.Resuest.REQUEST_PREVILEAGES, Const.Resuest.REQUEST_ENABLE_LOCATION -> {
                Log.v(Const.TAG, "MainActivity::onActivityResult resultCode = $resultCode")
                if (BleSetup.assert(this)) {
                    val service = Intent(this, BleService::class.java).apply {
                        this.action = Const.Action.START_SENSE
                    }
                    startService(service)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun bluetoothSpec() : SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        adapter?.run {
            builder.append("isMultipleAdvertisementSupported\n", strike{this.isMultipleAdvertisementSupported()}, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("isOffloadedScanBatchingSupported\n", strike{this.isOffloadedScanBatchingSupported()}, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("isOffloadedFilteringSupported\n", strike{this.isOffloadedFilteringSupported()}, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                builder.append("isLePeriodicAdvertisingSupported\n", strike{this.isLePeriodicAdvertisingSupported()}, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.append("isLeCodedPhySupported\n", strike{this.isLeCodedPhySupported()}, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.append("isLeExtendedAdvertisingSupported\n", strike{this.isLeExtendedAdvertisingSupported() }, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return builder
    }

    private fun strike(which : () -> Boolean) : Any {
        return if (which.invoke()) {
            StyleSpan(Typeface.NORMAL)
        } else {
            StrikethroughSpan()
        }
    }

    val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.v("TAG", "MainActivity::onServiceDisconnected()")
            bleService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.v("TAG", "MainActivity::onServiceConnected()")
            val binder = service as BleService.LocalBinder
            bleService = binder.getService()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val name = "status"
            val description = "information about current situation"
            val channelId = getString(R.string.not_channel_id)
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
            channel.description = description
            nm.createNotificationChannel(channel)
        }
    }
}
