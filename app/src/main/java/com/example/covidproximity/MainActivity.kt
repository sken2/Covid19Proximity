package com.example.covidproximity

import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.example.covidproximity.setup.BleSetup
import com.example.covidproximity.setup.NotificationSetup
import com.example.covidproximity.tasks.BleService
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private val adapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private val nm by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val backFab by lazy {
        findViewById(R.id.fab_back) as FloatingActionButton
    }
    private var isBound = false
    private val defaultMenuId = R.menu.option_menu
    private var menuId = defaultMenuId

    var bleService : BleService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(Const.TAG, "MainActivity::onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        backFab.setOnClickListener {
            findNavController(R.id.fragment_main).navigate(R.id.action_global_controlFragment)
        }
        NotificationSetup.createChannel(this)
        val sevice = Intent(this, BleService::class.java)
        startService(sevice)
    }

    override fun onNewIntent(intent: Intent?) {
        intent?.run {
            if (action == null) {
                Log.i(Const.TAG, "MainActivity::onNewIntent no action on intent")
            }
        }
        when (BleSetup.getState()) {
            BleSetup.Status.NEED_PRIVILAGE -> {
                var previleageNeeded = emptyArray<String>()
                for (previleage in BleSetup.preferPlivileges) {
                    if (ContextCompat.checkSelfPermission(this, previleage) != PackageManager.PERMISSION_GRANTED) {
                        previleageNeeded += previleage
                    }
                }
                if (previleageNeeded.isNotEmpty()) {
                    requestPermissions(previleageNeeded, Const.Requests.REQUEST_PREVILEAGES)
                }
            }
            else -> {}  // other request is issued by BleService
        }
        super.onNewIntent(intent)
    }

    override fun onResume() {
        Log.v(Const.TAG, "MainActivity::onResume")
        super.onResume()
        BleSetup.onPrevileageChanged(this)
        BleSetup.onLocationChanged(this)
        val service = Intent(this.applicationContext, BleService::class.java)
        isBound = applicationContext.bindService(service, connection, Service.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        Log.v(Const.TAG, "Mainactivity::onPause")
        if (isBound) {
            applicationContext.unbindService(connection)
        }
        super.onPause()
    }

    override fun onDestroy() {
        Log.v(Const.TAG, "MainAcctivity::onDestroy")
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            Const.Requests.REQUEST_PREVILEAGES, Const.Requests.REQUEST_ENABLE_LOCATION -> {
                Log.v(Const.TAG, "MainActivity::onActivityResult resultCode = $resultCode")
                BleSetup.onLocationChanged(this)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.v(Const.TAG, "MainActivity::onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(menuId, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.v(Const.TAG, "MainActivity::onOptionsItemSelected")
        when (item.itemId) {
            R.id.menu_settings -> {
                findNavController(R.id.fragment_main).navigate(R.id.action_global_settingsFragment)
                return true
            }
            else ->
                return false
//                return super.onOptionsItemSelected(item)
        }
    }

    fun setMenu(menuId : Int) {
        this.menuId = menuId
        invalidateOptionsMenu()
    }

    fun setDefaultMenu() {
        this.menuId = defaultMenuId
        invalidateOptionsMenu()
    }

    fun showBack(visible : Boolean = true) {
        backFab.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    fun bluetoothSpec() : SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        adapter?.run {
            builder.append("isMultipleAdvertisementSupported\n", strike{this.isMultipleAdvertisementSupported()}, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("isOffloadedScanBatchingSupported\n", strike{this.isOffloadedScanBatchingSupported()}, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("isOffloadedFilteringSupported\n", strike{this.isOffloadedFilteringSupported()}, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                builder.append("isLePeriodicAdvertisingSupported\n", strike{this.isLePeriodicAdvertisingSupported()}, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.append("isLeCodedPhySupported\n", strike{this.isLeCodedPhySupported()}, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.append("isLeExtendedAdvertisingSupported\n", strike{this.isLeExtendedAdvertisingSupported() }, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return builder
    }

    private fun strike(which : () -> Boolean) : Any {
        return if (which.invoke()) {
            StyleSpan(Typeface.NORMAL)
        } else {
            StrikethroughSpan()
        }
    }

    val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.v(Const.TAG, "MainActivity::onServiceDisconnected()")
            bleService = null
            isBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.v(Const.TAG, "MainActivity::onServiceConnected()")
            val binder = service as BleService.LocalBinder
            bleService = binder.getService()
        }
    }
}
