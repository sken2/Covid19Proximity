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
            result.add(
                Contact(
                    cursor.getString(cursor.getColumnIndex(History.COLUMN_NAME_TIME)),
                    cursor.getString(cursor.getColumnIndex(History.COLUMN_NAME_PROXYMITY_KEY))
                )
            )
        }
        cursor.close()
        return result
    }

    fun record(db : SQLiteDatabase, key : UUID) {
        val contact = Contact(key)
        val values = ContentValues().apply {
            put(History.COLUMN_NAME_TIME, contact.getDate())
            put(History.COLUMN_NAME_PROXYMITY_KEY, contact.getUuid())
        }
        val id = db.insert(History.TABLE_NAME, null, values)
        Log.v("TAG", "ContactHisory::record inserted $id")
    }

    class Contact() {
        lateinit var uuid : UUID
        lateinit var date : Date
        constructor(who : UUID, now : Date) : this() {
            uuid = who
            date = now
        }
        constructor(who : UUID) : this(who, Date()) {
        }
        constructor(dateString : String, uuidStgring : String) : this() {
            uuid = UUID.fromString(uuidStgring)
            sdf.parse(dateString)?.apply{
                this@Contact.date = this
            }
        }
        fun getDate() : String {
            return sdf.format(this.date)
        }
        fun getUuid() : String {
            return this.uuid.toString()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}