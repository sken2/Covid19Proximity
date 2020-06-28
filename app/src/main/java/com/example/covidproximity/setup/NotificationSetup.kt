package com.example.covidproximity.setup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import com.example.covidproximity.BleService
import com.example.covidproximity.Const
import com.example.covidproximity.MainActivity
import com.example.covidproximity.R
import com.example.covidproximity.entities.Covid19

object NotificationSetup {

    fun createChannel(context : Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            with(context.applicationContext) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val name = "status"
                val description = "information about current situation"
                val channelId = getString(R.string.not_channel_id)
                val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
                channel.description = description
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun newNotification(context : Context) : Notification {
        with(context.applicationContext) {
            val title = if (Covid19.isRunning()) {
                "Running"
            } else {
                "Standing by"
            }
            val intent = requestIntent(this)
            val builder =
                if (Build.VERSION.SDK_INT >= 26) {
                    val channelId = getString(R.string.not_channel_id)
                    Notification.Builder(this, channelId)
                } else {
                    Notification.Builder(this)
                }
                    .setSmallIcon(
                        if (Covid19.isRunning() )
                            R.drawable.ic_proximity
                        else
                            R.drawable.ic_proximity_off
                    )
                    .setTicker(title)
                    .setContentText(BleSetup.getState().toString())
                    .setContentIntent(intent)
            return builder.build()
        }
    }

    private fun buildText(appContext: Context) : SpannableStringBuilder {
        val builder = SpannableStringBuilder()
            .append("Advetiser : ")
            .append(if (Covid19.isAdvertising()) "Running" else "Stopped", StyleSpan(Typeface.NORMAL), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            .append(", Scanner : ")
            .append(if (Covid19.isScanning()) "Running" else "Stopped", StyleSpan(Typeface.NORMAL), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return builder
    }

    private fun requestIntent(appContext : Context) : PendingIntent {
        with (appContext) {
            when (BleSetup.getState()) {
                BleSetup.Status.OK -> {
                    return PendingIntent.getActivity(
                        this.applicationContext, Const.Resuest.REQUEST_CODE, Intent(this, MainActivity::class.java), PendingIntent.FLAG_CANCEL_CURRENT
                    )
                }
                BleSetup.Status.ADAPTER_IS_OFF -> {
                    return PendingIntent.getActivity(
                        this.applicationContext, Const.Resuest.REQUEST_CODE, Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), PendingIntent.FLAG_CANCEL_CURRENT
                    )
                }
                BleSetup.Status.LOCATION_IS_OFF -> {
                    return PendingIntent.getActivity(
                        this.applicationContext, Const.Resuest.REQUEST_CODE, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), PendingIntent.FLAG_CANCEL_CURRENT
                    )
                }
                BleSetup.Status.NEED_PRIVILAGE -> {
                    return PendingIntent.getActivity(
                        this.applicationContext, Const.Resuest.REQUEST_CODE, Intent(this, MainActivity::class.java), PendingIntent.FLAG_CANCEL_CURRENT
                    )
                }
                else -> {
                    Log.e(Const.TAG, "BleService::requestIntent state unimplemented")
                    return PendingIntent.getService(
                        this.applicationContext, Const.Resuest.REQUEST_CODE, Intent(this, BleService::class.java), PendingIntent.FLAG_CANCEL_CURRENT
                    )
                }
            }
        }
    }
}