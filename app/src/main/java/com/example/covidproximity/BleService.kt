package com.example.covidproximity

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.database.sqlite.SQLiteDatabase
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.covidproximity.setup.BleSetup
import java.util.*

class BleService : Service(), Observer {

    val CHANNEL_ID by lazy {
        getString(R.string.not_channel_id)
    }
    private val statusNotificationId = 1
    lateinit var db : ContactHistory.HistoryDB
    lateinit var history : SQLiteDatabase

    override fun onCreate() {
        Log.v(Const.TAG, "BleService::onCreate")
        super.onCreate()
        goForeground()
        if (!BleSetup.assert(this)) {
            BleSetup.ask(this)
        }
        db = ContactHistory.HistoryDB(this)
        history = db.writableDatabase
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(BleSetup.btReceiver, filter)
        Corona.Emitter.addObserver(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(Const.TAG, "BleService::onStartCommand")
        intent?.run {
            when (action) {
                Const.Action.START_SENSE -> {
//                    Corona.open()
                }
                else -> {

                }
            }
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        Corona.Emitter.deleteObserver(this)
        history.close()
        db.close()
        unregisterReceiver(BleSetup.btReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun update(o: Observable?, arg: Any?) {

        when {
            o is Corona.Emitter -> {
                val key = arg as UUID
                ContactHistory.record(history, key)
            }
            else -> {
                Log.e(Const.TAG, "BleService::update unknown update from $o")
            }
        }
        TODO("Not yet implemented")
    }

    private val btStatus = object : Observable() {

    }

    private fun goForeground() {
        val intent = PendingIntent.getActivity(this, 123456, Intent(this, MainActivity::class.java), 0)
        val title = if (Corona.isRunning()) {
            "Running"
        } else {
            "Standing by"
        }
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        builder.setSmallIcon(R.drawable.ic_proximity)
                .setTicker(title)
                .setContentIntent(intent)
                .build()
        startForeground(statusNotificationId, builder.build())
    }

    inner class LocalBinder() : Binder() {
        fun getService() : BleService {
            return this@BleService
        }
    }
    val binder = LocalBinder()
}
