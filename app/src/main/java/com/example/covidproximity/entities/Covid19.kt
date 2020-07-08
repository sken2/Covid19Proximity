package com.example.covidproximity.entities

import android.app.AlarmManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.preference.PreferenceManager
import com.example.covidproximity.Const
import com.example.covidproximity.models.ContactModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.Future

object Covid19 : Observable() {

    private val adapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private val scanner by lazy {
        adapter?.bluetoothLeScanner
    }
    private val advertiszer by lazy {
        adapter?.bluetoothLeAdvertiser
    }
    private val uuid = UUID.fromString(Const.UUID)
    private var scanning = false
    private var advertising = false
    private var contactCount = 0
    private val writeData = false

    fun getCount() : Int {
        return contactCount
    }

    fun isRunning() : Boolean {
        return scanning or advertising
    }

    fun isAdvertising() = advertising

    fun isScanning() = scanning

    fun startAdvertising(proximityKey : UUID) {
        if (!advertising) {
            adapter?.bluetoothLeAdvertiser?.startAdvertising(
                Advertisement.getSettings(),
                Advertisement.getData(proximityKey),
                Advertisement
            )
        }
    }

    fun stopAdvertising() {
        adapter?.bluetoothLeAdvertiser?.stopAdvertising(
            Advertisement
        )
    }

    fun startScanning() {
        if (!scanning) {
            adapter?.bluetoothLeScanner?.startScan(
                Scanning.getFilters(),
                Scanning.getSettings(),
                Scanning
            )
            scanning = true
            setChanged()
            notifyObservers()
        }
    }

    fun stopScanning() {
        adapter?.bluetoothLeScanner?.stopScan(
            Scanning
        )
        scanning = false
        setChanged()
        notifyObservers()
    }

    private fun onKeyCycleStarted() {
        advertising = true
        setChanged()
        notifyObservers()
    }

    private fun onKeyCycleStopped() {
        stopAdvertising()
        advertising = false
        setChanged()
        notifyObservers()
    }

    object Scanning : ScanCallback() {
        private val bf: ByteBuffer = ByteBuffer.allocate(16).apply {
            this.order(ByteOrder.BIG_ENDIAN)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {
                    Log.v(Const.TAG, "Covid19::onScanResult callbackType = CALLBACK_TYPE_ALL_MATCHES")
                    result?.scanRecord?.run {
                        val dump= this.bytes.joinToString(separator = " ") {
                            String.format("%02X", it)
                        }
                        Log.d(Const.TAG, "Covid19::onScanResult data = $dump")
                        val proximityKey = getKey(bytes)
                        val rxRssi = result.rssi
                        //TODO look for txpower from data
                        val txPower = if (Build.VERSION.SDK_INT >= 26) {
                            result.txPower
                        } else {
                            -50
                        }
                        Log.i(Const.TAG, "Covid19::onScanResult proximity key found $proximityKey")
                        KeyEmitter.run {
                            arrive(
                                ContactModel.Contact(proximityKey, txPower, rxRssi)
                            )
                        }
                    }
                    contactCount += 1
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(Const.TAG, "onScanFailed $errorCode")
            scanning = false
            setChanged()
            notifyObservers()
        }

        fun getFilters(): List<ScanFilter> {
            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(uuid))
                .build()
            return listOf(filter)
        }

        fun getSettings(): ScanSettings {
            return ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setMatchMode(ScanSettings.MATCH_NUM_FEW_ADVERTISEMENT)
                .build()
        }

        private fun getKey(data : ByteArray) : UUID {
            var point = 0
            while (point < data.size) {
                when (data[point + 1]) {    // fetch type
                    0x16.toByte() -> {
                        bf.rewind()
                        val keyPosition = point + 4
                        bf.put(data.copyOfRange(keyPosition, keyPosition+16))
                        return UUID(bf.getLong(0), bf.getLong(8))
                    }
                    else -> point += (data[point] + 1)
                }
            }
            return UUID(-1,-1)
        }
    }

    object Advertisement : AdvertiseCallback() {

        private val bf = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.v(Const.TAG,"Covid19::onStartSuccess")
            super.onStartSuccess(settingsInEffect)
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(Const.TAG, "Covid19::onStartFailer $errorCode")
            advertising = false
            setChanged()
            notifyObservers()
        }

        fun getSettings() : AdvertiseSettings {
            return AdvertiseSettings.Builder()
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
                .build()
        }

        fun getData(proximityKey : UUID) : AdvertiseData {
            val builder = AdvertiseData.Builder()
                    .addServiceUuid(ParcelUuid(uuid))
            proximityKey.run {
                bf.rewind()
                bf.put(19.toByte()) // 1+2+16
                bf.put(0x16.toByte())   // type UUID
                bf.put(0xfd.toByte()).put(0xf6.toByte())
                bf.putLong(this.mostSignificantBits).putLong(this.leastSignificantBits)
                builder.addServiceData(ParcelUuid(uuid), bf.array())
            }
            return builder.build()
        }
    }

    object KeyEmitter : Observable() {
        fun arrive(contact : ContactModel.Contact) {
            setChanged()
            notifyObservers(contact)
        }
    }

    object KeyDispenser : Observable(), AlarmManager.OnAlarmListener {

        private var am : AlarmManager? = null

        override fun onAlarm() {
            val key = newKey()
            Log.v(Const.TAG, "Covid19::onAlarm key = $key")
            setChanged()
            notifyObservers(key)
            am?.setWindow(AlarmManager.RTC,
                System.currentTimeMillis() + 15 * 60 * 1000,60 * 1000,
                "foo", this@KeyDispenser, Handler(Looper.getMainLooper())
            )
        }

        fun start(context : Context) {
            with(context) {
                if (context is Observer) {
                    addObserver(context)
                }
                am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                Covid19.onKeyCycleStarted()
            }
            onAlarm()
        }

        fun stop() {
            am?.cancel(this)
            onKeyCycleStopped()
            deleteObservers()
        }

        fun newKey() : UUID {
            return UUID.randomUUID()
        }
    }

    object PeriodicScanning : ScanCallback() {

        var receivedPackets = 0
        var droppedPacets = 0
        var periodFuture : Future<Unit>? = null
        val reduceQueue = mutableMapOf<String, ScanResult>()
        val executor = Executors.newSingleThreadExecutor()

        private val bf: ByteBuffer = ByteBuffer.allocate(16).apply {
            this.order(ByteOrder.BIG_ENDIAN)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            receivedPackets ++
            result?.run {
                val address = device.address
                if (reduceQueue.containsKey(address)) {
                    droppedPacets ++
                    return
                }
                reduceQueue.put(address, this)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.v(Const.TAG, "Covid19::onScanFailed errorCode = $errorCode")
            super.onScanFailed(errorCode)
            periodFuture?.cancel(false)
        }

        fun start() {
            scanning = true
            nextCycle()
            setChanged()
            notifyObservers()
        }

        fun stop() {
            scanning = false
            periodFuture?.cancel(true)
            setChanged()
            notifyObservers()
        }

        fun nextCycle() {
            periodFuture?.run {
                try {
                    get()
                } catch (e : CancellationException) {
                    Log.i(Const.TAG, "Covid19::nextCycle canceled")
                }
            }
            if (isScanning()) {
                periodFuture = executor.submit(PeriodicTask())
            } else {
                setChanged()
                notifyObservers()
            }
        }

        private fun getKey(data : ByteArray) : UUID {
            var point = 0
            while (point < data.size) {
                when (data[point + 1]) {    // fetch type
                    0x16.toByte() -> {
                        bf.rewind()
                        val keyPosition = point + 4
                        bf.put(data.copyOfRange(keyPosition, keyPosition+16))
                        return UUID(bf.getLong(0), bf.getLong(8))
                    }
                    else -> point += (data[point] + 1)
                }
            }
            return UUID(-1,-1)
        }

        class PeriodicTask() : Callable<Unit> {

            val handler = android.os.Handler(Looper.getMainLooper())

            override fun call(): Unit {
                reduceQueue.clear()
                scanner?.startScan(Scanning.getFilters(), Scanning.getSettings(), PeriodicScanning)
                Thread.sleep(1 * 1000)
                scanner?.stopScan(PeriodicScanning)
                Thread.sleep(100)
                reduceQueue.forEach { (_, u) ->
                    val dump= u.scanRecord?.bytes?.joinToString(separator = " ") {
                        String.format("%02X", it)
                    }
                    Log.d(Const.TAG, "Covid19::onScanResult data = $dump")
                    u.scanRecord?.bytes?.run {
                        val proximityKey = getKey(this)
                        val rxRssi = u.rssi
                        //TODO look for txpower from data
                        val txPower = if (Build.VERSION.SDK_INT >= 26) {
                            u.txPower
                        } else {
                            -50
                        }
                        Log.i(Const.TAG, "Covid19::onScanResult proximity key found $proximityKey")
                        KeyEmitter.run {
                            arrive(
                                ContactModel.Contact(proximityKey, txPower, rxRssi)
                            )
                        }
                    }
                }
                Thread.sleep(13 * 1000)
                if (Thread.interrupted()) return
                handler.post {
                    nextCycle()
                }
                return
            }
        }
    }
}