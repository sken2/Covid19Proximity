package com.example.covidproximity

import android.annotation.SuppressLint
import java.text.SimpleDateFormat

object Const {

    const val TAG = "CoronaProximity"

    object Preferences {
    }
    val UUID = "0000fd6f-0000-1000-8000-00805f9b34fb"

    object Requests {
        const val REQUEST_ENABLE_BT = 123
        const val REQUEST_PREVILEAGES = 456
        const val REQUEST_ENABLE_LOCATION = 789
        const val NOTIFY_FOREGROUND = 999
        const val REQUEST_CODE = 4649
    }

    object Keys {
        const val DetailMode = "detailMode"
        const val Duration = "duration"
    }

    @SuppressLint("SimpleDateFormat")
    val ISO8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}