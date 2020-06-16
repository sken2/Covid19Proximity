package com.example.covidproximity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import com.example.covidproximity.setup.BleSetup
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    val adapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private val nm by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    val backFab by lazy {
        findViewById(R.id.fab_back) as FloatingActionButton
    }
    var isBound = false

    var bleService : BleService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createChannel()

        setContentView(R.layout.activity_main)
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

    override fun onResume() {
        super.onResume()
        val service = Intent(this.applicationContext, BleService::class.java)
        isBound = applicationContext.bindService(service, connection, Service.BIND_AUTO_CREATE)
        if (!BleSetup.assert(this)) {
            BleSetup.ask(this)
        }
    }

    override fun onPause() {
        if (isBound) {
            applicationContext.unbindService(connection)
        }
        super.onPause()
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
            Log.v(Const.TAG, "MainActivity::onServiceDisconnected()")
            bleService = null
            isBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.v(Const.TAG, "MainActivity::onServiceConnected()")
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
