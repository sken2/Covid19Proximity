package com.example.covidproximity.adapters

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.covidproximity.Const
import com.example.covidproximity.models.ContactModel
import com.example.covidproximity.models.RoundKeyModel

class HistoryDBWrapper(context : Context)
    : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private val DATABASE_NAME = "history.db"
        private val DATABASE_VERSION = 2
    }

    override fun onCreate(db: SQLiteDatabase?) {
        Log.v(Const.TAG, "HistoryDBWrapper::onCreate")
        db?.execSQL(ContactModel.SQL_CREATE_ENTRIES)
        db?.execSQL(RoundKeyModel.SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.v(Const.TAG, "ContactHistory::onUpgrade oldVersion = $oldVersion newVersion = $newVersion")
        when (oldVersion) {
            1 -> {
//                db?.execSQL(ContactHistory.SQL_CREATE_ROUND_ENTRIES)
            }
            else -> {
                Log.e(Const.TAG, "ContactHistory::onUpgrade unknown version of current database: $oldVersion")
            }
        }
    }
}
