package com.example.covidproximity

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_database_maintenance.*
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class DatabaseMaintenanceActivity : AppCompatActivity() {

    lateinit var db : ContactHistory.HistoryDB
    lateinit var history : SQLiteDatabase
    val latch = CountDownLatch(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_maintenance)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            val wdb = db.writableDatabase
            ContactHistory.record(wdb, UUID.randomUUID())
            wdb.close()
        }
        db = ContactHistory.HistoryDB(this)
        thread {
            history = db.readableDatabase
            latch.countDown()
        }
    }

    override fun onResume() {
        super.onResume()
        latch.await()
        ContactHistory.getAll(history)
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }
}
