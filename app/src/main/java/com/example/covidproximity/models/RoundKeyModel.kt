package com.example.covidproximity.models

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import android.util.Log
import com.example.covidproximity.Const
import java.util.*

object RoundKeyModel {

    object RoundKeyTable : BaseColumns {
        const val TABLE_NAME = "round"
        const val COLUMN_NAME_TIME = "time"
        const val COLUMN_NAME_ROUNDAROUND_KEY = "round_key"
    }
    const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${RoundKeyTable.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${RoundKeyTable.COLUMN_NAME_TIME} TEXT not NULL, " +
                "${RoundKeyTable.COLUMN_NAME_ROUNDAROUND_KEY} TEXT not NULL)"

    const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${RoundKeyTable.TABLE_NAME}"

    fun recore(db : SQLiteDatabase, roundKey : RoundKey) {
        val values = ContentValues().apply {
            val isoDate = Const.ISO8601.format(roundKey.used)
            put(RoundKeyTable.COLUMN_NAME_TIME, isoDate)
            put(RoundKeyTable.COLUMN_NAME_ROUNDAROUND_KEY, roundKey.key.toString())
        }
        val id = db.insert(RoundKeyTable.TABLE_NAME, null, values)
        Log.v("TAG", "RoundKeyModel::record inserted $id")
    }

    class RoundKey() {
        lateinit var key : UUID
        lateinit var used : Date
        constructor(uuid : String) : this() {
            key = UUID.fromString(uuid)
            used = Date()
        }
        constructor(uuid: String, useFrom: String) : this() {
            key = UUID.fromString(uuid)
            Const.ISO8601.parse(useFrom)?.let {
                used = it
            }//TODO do something when parse was failed
        }
    }
}