package com.example.covidproximity

import android.annotation.SuppressLint
import java.text.SimpleDateFormat

object Const {

    val TAG = "CoronaProximity"
    val PREFERENCE_NAME = "CP"

    object Preferences {
        val AUTO_ADVERTISE = "auto_Advertise"
        val AUTO_SCAN = "auto_scan"
    }
    val UUID = "0000fd6f-0000-1000-8000-00805f9b34fb"

    object Requests {
        val REQUEST_ENABLE_BT = 123
        val REQUEST_PREVILEAGES = 456
        val REQUEST_ENABLE_LOCATION = 789
        val NOTIFY_FOREGROUND = 999
        val REQUEST_CODE = 4649
    }

    @SuppressLint("SimpleDateFormat")
    val ISO8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}