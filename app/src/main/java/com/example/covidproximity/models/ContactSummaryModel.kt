package com.example.covidproximity.models

import android.database.sqlite.SQLiteDatabase
import android.os.Build
import com.example.covidproximity.Const
import java.lang.Math.sqrt
import java.time.LocalDate
import java.util.*


object ContactSummaryModel {

    object ContactCountTable {
        const val COLUNM_KEY_COUNT = "key_count"
    }

    fun getTodaysSummary(db: SQLiteDatabase) : List<ContactSummary> {
        val todaysHitory = ContactModel.getToday(db)
        return distinctMap(todaysHitory)
    }

    fun distinctMap(history : List<ContactModel.Contact>) :List<ContactSummary> {
        val result = mutableListOf<ContactSummary>()
        val keyList = history.groupBy { it.uuid }
        var minRssiDistance = 99.0
        keyList.forEach { t, u ->
            val count = u.size
            val dateBegin = u.first().date
            val dateEnd = u.last().date
//            minRssiDistance = u.minBy { it.rxRssi - it.txPower }.let {
//                val boo = (it.rxRssi - it.txPower).toDouble()
//                sqrt(boo * boo)
//            }
            val averageDistance = 99.0 // TODO
            result.add(ContactSummary(t, dateBegin, dateEnd, count, 95.0, averageDistance))
        }
        return result
    }

    data class ContactSummary(
        val key : UUID,
        val since : Date,
        val till : Date,
        val count : Int,
        val closestDistance : Double,
        val averageDistance : Double
    )
}