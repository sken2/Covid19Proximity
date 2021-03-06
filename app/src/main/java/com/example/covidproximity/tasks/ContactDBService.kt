package com.example.covidproximity.tasks

import android.app.Service
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.covidproximity.Const
import com.example.covidproximity.adapters.HistoryDBWrapper
import com.example.covidproximity.models.ContactModel
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors

class ContactDBService : Service(), Observer {

    private val executor = Executors.newFixedThreadPool(2)
    lateinit var db : HistoryDBWrapper
    lateinit var contact : SQLiteDatabase
    val resultDispenser = ResultDispenser()

    override fun update(o: Observable?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        db = HistoryDBWrapper(this)
        contact = db.readableDatabase
    }

    override fun onDestroy() {
        resultDispenser.deleteObservers()
        contact.close()
        db.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.v(Const.TAG, "ContactDBService::onBind")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.v(Const.TAG, "ContactDBService::onUnbind")
        return super.onUnbind(intent)
    }

    fun getAllHistory() {
        val future = executor.submit{
            val result = ContactModel.getAll(contact)
            resultDispenser.complete(result)
        }
        future.get()
    }

    fun <T>access(query : (SQLiteDatabase) -> T) {
        val future = executor.submit {
            val result = query.invoke(contact)
            resultDispenser.complete(result as Any)
        }
        try {
            future.get()
        } catch (e : CancellationException) {
            Log.i(Const.TAG, "")
        }
    }

    class ResultDispenser() : Observable() {
        fun complete(result : Any) {
            setChanged()
            Handler(Looper.getMainLooper()).post {
                notifyObservers(result)
            }
        }
    }

    inner class LocalBinder() : Binder() {
        fun getService() : ContactDBService {
            return this@ContactDBService
        }
    }
    val binder = LocalBinder()
}