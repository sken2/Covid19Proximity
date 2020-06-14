package com.example.covidproximity

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

object Corona {

    val adapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    val scanner by lazy {
        adapter?.bluetoothLeScanner
    }
    val advertiszer by lazy {
        adapter?.bluetoothLeAdvertiser
    }
    private val uuid = UUID.fromString(Const.UUID)
    private var scanning = false
    private var advertising = false
    private var contactCount = 0

    fun getCount() : Int {
        return contactCount
    }

    fun isRunning() : Boolean {
        return scanning and advertising
    }

    fun isAdvertising() = advertising

    fun isScanning() = scanning

    fun startAdvertising() {
        adapter?.bluetoothLeAdvertiser?.startAdvertising(Advertisement.getSettings(), Advertisement.getData(), Advertisement)
        advertising = true
    }

    fun stopAdvertising() {
        adapter?.bluetoothLeAdvertiser?.stopAdvertising(Advertisement)
        advertising = false
    }

    fun startScanning() {
        adapter?.bluetoothLeScanner?.startScan(Scanning.getFilters(), Scanning.getSettings(), Scanning)
        scanning = true
    }

    fun stopScanning() {
        adapter?.bluetoothLeScanner?.stopScan(Scanning)
        scanning = false
    }

    object Scanning : ScanCallback() {
        val bf = ByteBuffer.allocate(16).apply {
            this.order(ByteOrder.BIG_ENDIAN)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {
                    Log.v(Const.TAG, "Corona::onScanResult callbackType = CALLBACK_TYPE_ALL_MATCHES")
                    result?.scanRecord?.run {
                        val proximityKey = getKey(bytes)
                        Log.i(Const.TAG, "Corona::onScanResult proximity key found $proximityKey")
                        Emitter.run {
                            arrive(proximityKey)
                        }
                    }
                    contactCount += 1
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(Const.TAG, "onScanFailed $errorCode")
            scanning = false
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
                        val keyPosition = point + 4
                        bf.put(data.copyOfRange(keyPosition, keyPosition+15))
                        return UUID(bf.getLong(0), bf.getLong(8))
                    }
                    else -> point += (data[point] + 1)
                }
            }
            return UUID(-1,-1)
        }
    }

    object Advertisement : AdvertiseCallback() {

        var proximityKey : UUID? = null
        val bf = ByteBuffer.allocate(20).let {
            it.order(ByteOrder.BIG_ENDIAN)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(Const.TAG, "Corona::onStgartFailer $errorCode")
            advertising = false
        }

        fun getSettings() : AdvertiseSettings {
            return AdvertiseSettings.Builder()
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
                .build()
        }

        fun getData() : AdvertiseData {
            val builder = AdvertiseData.Builder()
                    .addServiceUuid(ParcelUuid(uuid))
            proximityKey?.run {
                bf.put(19.toByte()) // 1+2+16
                bf.put(0x16.toByte())   // type UUID
                bf.put(0xfd.toByte()).put(0xf6.toByte())
                bf.putLong(this.mostSignificantBits).putLong(this.leastSignificantBits)
                builder.addServiceData(ParcelUuid(uuid), bf.array())
            }
            return builder.build()
        }
    }

    object Emitter : Observable() {
        fun arrive(key : UUID) {
            setChanged()
            notifyObservers(key)
        }
    }
}