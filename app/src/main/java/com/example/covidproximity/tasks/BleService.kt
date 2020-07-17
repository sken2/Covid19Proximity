package com.example.covidproximity.tasks

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.example.covidproximity.Const
import com.example.covidproximity.R
import com.example.covidproximity.adapters.HistoryDBWrapper
import com.example.covidproximity.entities.Covid19
import com.example.covidproximity.models.ContactModel
import com.example.covidproximity.models.RoundKeyModel
import com.example.covidproximity.setup.BleSetup
import com.example.covidproximity.setup.NotificationSetup
import java.util.*

class BleService : Service(), Observer {

    lateinit var db : HistoryDBWrapper
    lateinit var contactDb : SQLiteDatabase
    private val preferences : SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    override fun onCreate() {
        Log.v(Const.TAG, "BleService::onCreate")
        super.onCreate()
        goForeground()
        db = HistoryDBWrapper(this)
        contactDb = db.writableDatabase
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(BleSetup.btReceiver, filter)
        Covid19.KeyEmitter.addObserver(this)
        BleSetup.addObserver(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(Const.TAG, "BleService::onStartCommand")
        intent?.run {
            when (BleSetup.getState()) {
                BleSetup.Status.OK -> {
                   if (preferences.getBoolean(getString(R.string.key_auto_advertise), false)) {
                        Covid19.KeyDispenser.start(this@BleService)
                    }
                    if (preferences.getBoolean(getString(R.string.key_auto_scan), false)) {
                        Covid19.startScanning()
                    }
                }
                BleSetup.Status.NO_ADAPTER, BleSetup.Status.NO_BLE_FUTURE -> {
                    shutdown()
                }
                else -> {}
            }
            goForeground()
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.v(Const.TAG, "BleService::onDestroy")
        BleSetup.deleteObserver(this)
        Covid19.KeyEmitter.deleteObserver(this)
        contactDb.close()
        db.close()
        unregisterReceiver(BleSetup.btReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        Log.v(Const.TAG, "BleService::onBind")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.v(Const.TAG, "BleService::onUnbind")
        if (!Covid19.isRunning()) {
            stopForeground(true)
        }
        return super.onUnbind(intent)
    }

    override fun update(o: Observable?, arg: Any?) {
        when {
            o is Covid19.KeyEmitter -> {
                val contact = arg as ContactModel.Contact
                ContactModel.record(contactDb, contact)
            }
            o is Covid19.KeyDispenser -> {
                val newKey = arg as UUID
                if (Covid19.isAdvertising()) {
                    Covid19.stopAdvertising()
                }
                RoundKeyModel.recored(contactDb, RoundKeyModel.RoundKey(newKey.toString()))
                Covid19.startAdvertising(newKey)
            }
            o is BleSetup -> {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(Const.Requests.NOTIFY_FOREGROUND, NotificationSetup.newNotification(this))
            }
            else -> {
                Log.e(Const.TAG, "BleService::update unknown update from $o")
            }
        }
    }

    fun startAdvertisze() {
        Covid19.KeyDispenser.start(this)
    }

    fun stopAdvertise() {
        Covid19.KeyDispenser.stop()
    }

    fun startScanning() {
        Covid19.PeriodicScanning.start()
    }

    fun stopScanning() {
        Covid19.PeriodicScanning.stop()
    }

    fun shutdown() {
        stopSelf()
        with(NotificationManagerCompat.from(this)) {
            cancel(Const.Requests.NOTIFY_FOREGROUND)
        }
    }

    private fun goForeground() {
        Log.v(Const.TAG, "BleService::goForeground")
        startForeground(Const.Requests.NOTIFY_FOREGROUND, NotificationSetup.newNotification(this))
    }

    inner class LocalBinder() : Binder() {
        fun getService() : BleService {
            return this@BleService
        }
    }
    val binder = LocalBinder()
}
