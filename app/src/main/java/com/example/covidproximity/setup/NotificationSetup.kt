package com.example.covidproximity.setup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.covidproximity.R

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
}