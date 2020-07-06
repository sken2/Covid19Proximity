package com.example.covidproximity.models

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.provider.BaseColumns
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.covidproximity.Const
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

object ContactModel {

    object ContactTable : BaseColumns {
        const val TABLE_NAME = "history"
        const val COLUMN_NAME_TIME = "time"
        const val COLUMN_NAME_PROXYMITY_KEY = "proximity_key"
        const val COLUMN_NAME_TX_POWER = "tx_power"
        const val COLUMN_NAME_RX_RSSI = "rx_rssi"
    }

    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${ContactTable.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${ContactTable.COLUMN_NAME_TIME} TEXT not NULL," +
                "${ContactTable.COLUMN_NAME_PROXYMITY_KEY} TEXT not NULL, " +
                "${ContactTable.COLUMN_NAME_TX_POWER} INTEGER, " +
                "${ContactTable.COLUMN_NAME_RX_RSSI} INTEGER)"

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${ContactTable.TABLE_NAME}"
    // version 1->2

    fun getAll(db : SQLiteDatabase) : List<Contact>{
        val result = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactTable.COLUMN_NAME_TIME,
            ContactTable.COLUMN_NAME_PROXYMITY_KEY,
            ContactTable.COLUMN_NAME_TX_POWER,
            ContactTable.COLUMN_NAME_RX_RSSI
        )
        val sortOrder = "${ContactTable.COLUMN_NAME_TIME} desc"
        val cursor = db.query(ContactTable.TABLE_NAME, projection, null, null, null, null, sortOrder)
        while (cursor.moveToNext()) {
            val uuid = UUID.fromString(cursor.getString(cursor.getColumnIndex(ContactTable.COLUMN_NAME_PROXYMITY_KEY)))
            result.add(
                Contact(
                    uuid,
                    cursor.getString(cursor.getColumnIndex(ContactTable.COLUMN_NAME_TIME)),
                    cursor.getInt(cursor.getColumnIndex(ContactTable.COLUMN_NAME_TX_POWER)),
                    cursor.getInt(cursor.getColumnIndex(ContactTable.COLUMN_NAME_RX_RSSI))
                )
            )
        }
        cursor.close()
        return result
    }

    fun getToday(db : SQLiteDatabase) : List<Contact> {
        if (Build.VERSION.SDK_INT >=26) {
            val now = LocalDate.now()
            val start = now.atStartOfDay()
            val end = start.plusDays(1)
            return getWhile(db, start.toInstant(ZoneOffset.UTC).toString() ,end.toInstant(ZoneOffset.UTC).toString())
        } else {
            return emptyList()  //　TODO off course
        }
    }

    fun getWhile(db: SQLiteDatabase, since : String, till : String) : List<Contact> {
        val result = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactTable.COLUMN_NAME_TIME,
            ContactTable.COLUMN_NAME_PROXYMITY_KEY,
            ContactTable.COLUMN_NAME_TX_POWER,
            ContactTable.COLUMN_NAME_RX_RSSI
        )
        val selection =
            "${ContactTable.COLUMN_NAME_TIME} BETWEEN  '$since' and '$till'"
//        val selectionArgs = arrayOf("between",  Const.ISO8601.format(since), "and", Const.ISO8601.format(till) )
        val sortOrder = "${ContactTable.COLUMN_NAME_TIME} desc"
        val cursor = db.query(ContactTable.TABLE_NAME, projection, selection, null,null, null, sortOrder)
        while (cursor.moveToNext()) {
            val uuid = UUID.fromString(cursor.getString(cursor.getColumnIndex(ContactTable.COLUMN_NAME_PROXYMITY_KEY)))
            result.add(
                Contact(
                    uuid,
                    cursor.getString(cursor.getColumnIndex(ContactTable.COLUMN_NAME_TIME)),
                    cursor.getInt(cursor.getColumnIndex(ContactTable.COLUMN_NAME_TX_POWER)),
                    cursor.getInt(cursor.getColumnIndex(ContactTable.COLUMN_NAME_RX_RSSI))
                )
            )
        }
        cursor.close()
        return result
    }

    fun record(db : SQLiteDatabase, contact : Contact) {
        val values = ContentValues().apply {
            val isoDate = Const.ISO8601.format(contact.date)
            put(ContactTable.COLUMN_NAME_TIME, isoDate)
            put(ContactTable.COLUMN_NAME_PROXYMITY_KEY, contact.uuid.toString())
            put(ContactTable.COLUMN_NAME_TX_POWER, contact.txPower)
            put(ContactTable.COLUMN_NAME_RX_RSSI, contact.rxRssi)
        }
        val id = db.insert(ContactTable.TABLE_NAME, null, values)
        Log.v("TAG", "ContactModel::record inserted $id")
    }

    fun record(db : SQLiteDatabase, key : UUID, txPower : Int, rxRssi : Int) {
        val contact = Contact(
            key,
            txPower,
            rxRssi
        )
        val values = ContentValues().apply {
            val isoDate = Const.ISO8601.format(contact.date)
            put(ContactTable.COLUMN_NAME_TIME, isoDate)
            put(ContactTable.COLUMN_NAME_PROXYMITY_KEY, contact.uuid.toString())

        }
        val id = db.insert(ContactTable.TABLE_NAME, null, values)
        Log.v("TAG", "ContactModel::record inserted $id")
    }

    class Contact() {
        lateinit var uuid : UUID
        lateinit var date : Date
        var txPower : Int = 0
        var rxRssi : Int = 0
        constructor(key : UUID, now : Date, txPower : Int, rxRssi : Int) : this() {
            uuid = key
            date = now
            this.txPower = txPower
            this.rxRssi = rxRssi
        }
        constructor(key : UUID, txPower: Int, rxRssi : Int) : this(key, Date(), txPower, rxRssi) {
        }
        constructor(key : UUID, isoDate : String, txPower : Int, rxRssi : Int) : this() {
            uuid = key
            Const.ISO8601.parse(isoDate)?.run {
                this@Contact.date = this
            }
            this.txPower = txPower
            this.rxRssi = rxRssi
        }
    }
}