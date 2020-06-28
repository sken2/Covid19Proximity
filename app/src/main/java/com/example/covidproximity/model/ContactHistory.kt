package com.example.covidproximity.model

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import com.example.covidproximity.Const
import java.text.SimpleDateFormat
import java.util.*

object ContactHistory {

    object History : BaseColumns {
        const val TABLE_NAME = "history"
        const val COLUMN_NAME_TIME = "time"
        const val COLUMN_NAME_PROXYMITY_KEY = "proximity_key"
        const val COLUMN_NAME_TX_POWER = "tx_power"
        const val COLUMN_NAME_RX_RSSI = "rx_rssi"
    }

    private const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${History.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${History.COLUMN_NAME_TIME} TEXT not NULL," +
                "${History.COLUMN_NAME_PROXYMITY_KEY} TEXT not NULL, " +
                "${History.COLUMN_NAME_TX_POWER} INTEGER, " +
                "${History.COLUMN_NAME_RX_RSSI} INTEGER)"

    private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${History.TABLE_NAME}"
    // version 1->2
    object RoundKey : BaseColumns {
        const val TABLE_NAME = "round"
        const val COLUMN_NAME_TIME = "time"
        const val COLUMN_NAME_PROXYMITY_KEY = "round_key"
    }
    private const val SQL_CREATE_ROUND_ENTRIES =
        "CREATE TABLE ${RoundKey.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${RoundKey.COLUMN_NAME_TIME} TEXT not NULL, " +
                "${RoundKey.COLUMN_NAME_PROXYMITY_KEY} TEXT not NULL)"

    private const val SQL_DELETE_ROUND_ENTRIES = "DROP TABLE IF EXISTS ${RoundKey.TABLE_NAME}"

    private val DATABASE_NAME = "history.db"
    private val DATABASE_VERSION = 2

    class HistoryDB(context : Context) : SQLiteOpenHelper(context,
        DATABASE_NAME, null,
        DATABASE_VERSION
    ) {

        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL(SQL_CREATE_ENTRIES)
            db?.execSQL(SQL_CREATE_ROUND_ENTRIES)
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            Log.v(Const.TAG, "ContactHistory::onUpgrade oldVersion = $oldVersion newVersion = $newVersion")
            when (oldVersion) {
                1 -> {
                    db?.execSQL(SQL_CREATE_ROUND_ENTRIES)
                }
                else -> {
                    Log.e(Const.TAG, "ContactHistory::onUpgrade unknown version of current database: $oldVersion")
                }
            }
        }
    }

    fun getAll(db : SQLiteDatabase) : List<Contact>{
        val result = mutableListOf<Contact>()
        val projection = arrayOf(
            History.COLUMN_NAME_TIME,
            History.COLUMN_NAME_PROXYMITY_KEY,
            History.COLUMN_NAME_TX_POWER,
            History.COLUMN_NAME_RX_RSSI
        )
        val sortOrder = "${History.COLUMN_NAME_TIME} desc"
        val cursor = db.query(History.TABLE_NAME, projection, null, null, null, null, sortOrder)
        while (cursor.moveToNext()) {
            val uuid = UUID.fromString(cursor.getString(cursor.getColumnIndex(History.COLUMN_NAME_PROXYMITY_KEY)))
            result.add(
                Contact(
                    uuid,
                    cursor.getString(cursor.getColumnIndex(History.COLUMN_NAME_TIME)),
                    cursor.getInt(cursor.getColumnIndex(History.COLUMN_NAME_TX_POWER)),
                    cursor.getInt(cursor.getColumnIndex(History.COLUMN_NAME_RX_RSSI))
                )
            )
        }
        cursor.close()
        return result
    }

    fun getToday(db : SQLiteDatabase) : List<Contact> {
        val now = Date()
        val start = now.apply {
            time = 0
            minutes = 0
            seconds = 0
        }
        val end = start.apply {
            date += 1
        }
        return getWhile(
            db,
            start,
            end
        )
    }

    fun getWhile(db: SQLiteDatabase, since : Date, till : Date) : List<Contact> {
        val result = mutableListOf<Contact>()
        val projection = arrayOf(
            History.COLUMN_NAME_TIME,
            History.COLUMN_NAME_PROXYMITY_KEY,
            History.COLUMN_NAME_TX_POWER,
            History.COLUMN_NAME_RX_RSSI
        )
        val selection =
            History.COLUMN_NAME_TIME
        val selectionArgs = arrayOf("between",  sdf.format(since), "and", sdf.format(till) )
        val sortOrder = "${History.COLUMN_NAME_TIME} desc"
        val cursor = db.query(History.TABLE_NAME, projection, selection, selectionArgs,null, null, sortOrder)
        while (cursor.moveToNext()) {
            val uuid = UUID.fromString(cursor.getString(cursor.getColumnIndex(History.COLUMN_NAME_PROXYMITY_KEY)))
            result.add(
                Contact(
                    uuid,
                    cursor.getString(cursor.getColumnIndex(History.COLUMN_NAME_TIME)),
                    cursor.getInt(cursor.getColumnIndex(History.COLUMN_NAME_TX_POWER)),
                    cursor.getInt(cursor.getColumnIndex(History.COLUMN_NAME_RX_RSSI))
                )
            )

        }
        cursor.close()
        return result
    }

    fun record(db : SQLiteDatabase, contact : Contact) {
        val values = ContentValues().apply {
            val isoDate = sdf.format(contact.date)
            put(History.COLUMN_NAME_TIME, isoDate)
            put(History.COLUMN_NAME_PROXYMITY_KEY, contact.uuid.toString())
            put(History.COLUMN_NAME_TX_POWER, contact.txPower)
            put(History.COLUMN_NAME_RX_RSSI, contact.rxRssi)
        }
        val id = db.insert(History.TABLE_NAME, null, values)
        Log.v("TAG", "ContactHisory::record inserted $id")
    }
    fun record(db : SQLiteDatabase, key : UUID, txPower : Int, rxRssi : Int) {
        val contact = Contact(
            key,
            txPower,
            rxRssi
        )
        val values = ContentValues().apply {
            val isoDate = sdf.format(contact.date)
            put(History.COLUMN_NAME_TIME, isoDate)
            put(History.COLUMN_NAME_PROXYMITY_KEY, contact.uuid.toString())

        }
        val id = db.insert(History.TABLE_NAME, null, values)
        Log.v("TAG", "ContactHisory::record inserted $id")
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
            sdf.parse(isoDate)?.run {
                this@Contact.date = this
            }
            this.txPower = txPower
            this.rxRssi = rxRssi
        }
    }

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}