package com.example.covidproximity.models

import android.database.sqlite.SQLiteDatabase
import java.lang.Math.log
import java.util.*


object ContactSummaryModel {

    object ContactCountTable {
        const val COLUNM_KEY_COUNT = "key_count"
    }

    fun getTodaysSummary(db: SQLiteDatabase) : List<ContactSummary> {
        val todaysHitory = ContactModel.getToday(db)
        return distinctList(todaysHitory)
    }

    fun getOneWeekSummary(db : SQLiteDatabase) : List<ContactSummary> {
        val todaysHitory = ContactModel.getOneWeek(db)
        return distinctList(todaysHitory)
    }

    fun getTwoWeekSummary(db : SQLiteDatabase) : List<ContactSummary> {
        val todaysHitory = ContactModel.getTwoWeeks(db)
        return distinctList(todaysHitory)
    }

    fun distinctList(history : List<ContactModel.Contact>) :List<ContactSummary> {
        val result = mutableListOf<ContactSummary>()
        val keyList = history.groupBy { it.uuid }
        keyList.forEach { t, u ->
            var minRssiDistance = 99.0
            val count = u.size
            val dateBegin = u.last().date
            val dateEnd = u.first().date
            val minEntry = u.minBy { it.rxRssi - it.txPower }
            minEntry?.run {
                minRssiDistance = distance(rxRssi, txPower)
            }
            val averageDistance = 99.0 // TODO
            result.add(ContactSummary(t, dateBegin, dateEnd, count, minRssiDistance, averageDistance))
        }
        return result
    }

    private fun distance(rx : Int, tx : Int) : Double {
        return log( (rx - tx) * (rx - tx).toDouble())
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