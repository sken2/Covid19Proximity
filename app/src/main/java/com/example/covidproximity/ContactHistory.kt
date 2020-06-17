package com.example.covidproximity

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object ContactHistory {

    object History : BaseColumns {
        const val TABLE_NAME = "history"
        const val COLUMN_NAME_TIME = "time"
        const val COLUMN_NAME_PROXYMITY_KEY = "proximity_key"
    }

    private const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${History.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${History.COLUMN_NAME_TIME} TEXT not NULL," +
                "${History.COLUMN_NAME_PROXYMITY_KEY} TEXT not NULL)"

    private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${History.TABLE_NAME}"

    val DATABASE_NAME = "history.db"
    private val DATABASE_VERSION = 1

    class HistoryDB(context : Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL(SQL_CREATE_ENTRIES)
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            db?.execSQL(SQL_DELETE_ENTRIES)
            onCreate(db)
        }
    }

    fun getAll(db : SQLiteDatabase) : List<Contact>{
        val result = mutableListOf<Contact>()
        val projection = arrayOf(History.COLUMN_NAME_TIME, History.COLUMN_NAME_PROXYMITY_KEY)
        val sortOrder = "${History.COLUMN_NAME_TIME} desc"
        val cursor = db.query(History.TABLE_NAME, projection, null, null, null, null, sortOrder)
        while (cursor.moveToNext()) {
            val uuid = UUID.fromString(cursor.getString(cursor.getColumnIndex(History.COLUMN_NAME_PROXYMITY_KEY)))
            result.add(
                Contact(
                    uuid,
                    cursor.getString(cursor.getColumnIndex(History.COLUMN_NAME_TIME))
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
        return getWhile(db, start, end)
    }

    fun getWhile(db: SQLiteDatabase, since : Date, till : Date) : List<Contact> {
        val result = mutableListOf<Contact>()
        val projection = arrayOf(History.COLUMN_NAME_TIME, History.COLUMN_NAME_PROXYMITY_KEY)
        val selection = History.COLUMN_NAME_TIME
        val selectionArgs = arrayOf("between",  sdf.format(since), "and", sdf.format(till) )
        val sortOrder = "${History.COLUMN_NAME_TIME} desc"
        val cursor = db.query(History.TABLE_NAME, projection, selection, selectionArgs,null, null, sortOrder)
        while (cursor.moveToNext()) {
            val uuid = UUID.fromString(cursor.getString(cursor.getColumnIndex(History.COLUMN_NAME_PROXYMITY_KEY)))
            result.add(
                Contact(
                    uuid,
                    cursor.getString(cursor.getColumnIndex(History.COLUMN_NAME_TIME))
                )
            )
        }
        cursor.close()
        return result
    }

    fun record(db : SQLiteDatabase, key : UUID) {
        val contact = Contact(key)
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
        constructor(key : UUID, now : Date) : this() {
            uuid = key
            date = now
        }
        constructor(key : UUID) : this(key, Date()) {
        }
        constructor(key : UUID, isoDate : String) : this() {
            uuid = key
            sdf.parse(isoDate).run {
                this@Contact.date = this
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}