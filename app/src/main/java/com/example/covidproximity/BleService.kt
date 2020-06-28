package com.example.covidproximity

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
import com.example.covidproximity.entities.Covid19
import com.example.covidproximity.models.ContactHistory
import com.example.covidproximity.setup.BleSetup
import com.example.covidproximity.setup.NotificationSetup
import java.util.*

class BleService : Service(), Observer {

    lateinit var db : ContactHistory.HistoryDB
    lateinit var history : SQLiteDatabase
    private val preferences : SharedPreferences by lazy {
        this.applicationContext.getSharedPreferences(Const.PREFERENCE_NAME,Context.MODE_PRIVATE)
    }
    private val nm by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE)
    }

    override fun onCreate() {
        Log.v(Const.TAG, "BleService::onCreate")
        super.onCreate()
        goForeground()
        db = ContactHistory.HistoryDB(this)
        history = db.writableDatabase
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
                    if (preferences.getBoolean(Const.Preferences.AUTO_ADVERTISE, false)) {
                        Covid19.startAdvertising()
                    }
                    if (preferences.getBoolean(Const.Preferences.AUTO_SCAN, false)) {
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
        history.close()
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
        if (!Covid19.isAdvertising() and !Covid19.isRunning()) {
            stopForeground(true)
        }
        return super.onUnbind(intent)
    }

    override fun update(o: Observable?, arg: Any?) {
        when {
            o is Covid19.KeyEmitter -> {
                val contact = arg as ContactHistory.Contact
                ContactHistory.record(history, contact)
            }
            o is BleSetup -> {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(Const.Resuest.NOTIFY_FOREGROUND, NotificationSetup.newNotification(this))
            }
            else -> {
                Log.e(Const.TAG, "BleService::update unknown update from $o")
            }
        }
    }

    fun shutdown() {
        stopSelf()
        with(NotificationManagerCompat.from(this)) {
            cancel(Const.Resuest.NOTIFY_FOREGROUND)
        }
    }

    private fun goForeground() {
        Log.v(Const.TAG, "BleService::goForeground")
        startForeground(Const.Resuest.NOTIFY_FOREGROUND, NotificationSetup.newNotification(this))
    }

    inner class LocalBinder() : Binder() {
        fun getService() : BleService {
            return this@BleService
        }
    }
    val binder = LocalBinder()
}
