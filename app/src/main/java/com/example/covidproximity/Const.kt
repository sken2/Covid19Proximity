package com.example.covidproximity

object Const {

    val TAG = "CoronaProximity"

    val UUID = "0000fd6f-0000-1000-8000-00805f9b34fb"

    object Action {
        val REVISE_PRIVILAGE = "coronaproximity_ACTION_REVISE_PRIVILAGE"
        val START_SENSE = "coronaproximity_ACTION_START_SENSE"
    }

    object Extras {
        val CALLER = "caller"
    }

    object Resuest {
        val REQUEST_ENABLE_BT = 123
        val REQUEST_PREVILEAGES = 456
        val REQUEST_ENABLE_LOCATION = 789
        val NOTIFY_FOREGROUND = 999
    }
}