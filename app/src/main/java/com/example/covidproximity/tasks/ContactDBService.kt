package com.example.covidproximity.tasks

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class ContactDBService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    inner class LocalBinder() : Binder() {
        fun getService() : ContactDBService {
            return this@ContactDBService
        }
    }
    val binder = LocalBinder()
}