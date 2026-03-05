package com.example.viyo

import android.annotation.SuppressLint
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import kotlin.experimental.and
import kotlin.math.pow

class BeaconScanner(
    private val scanner: BluetoothLeScanner?,
    private val listener: BeaconListener
) {
    private val kalmanFilters = HashMap<String, KalmanFilter>()
    private val lastUpdateMap = HashMap<String, Long>()
    private val TARGET_IBEACON_UUID = "abcdef01-2345-6789-8765-43210fedcbaa"

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (scanner == null) return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        scanner?.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanRecord = result.scanRecord ?: return
            val device = result.device
            val rssi = result.rssi
            val address = device.address

            val manufacturerData = scanRecord.getManufacturerSpecificData(76)
            if (manufacturerData == null || manufacturerData.size < 23) return
            if (manufacturerData[0] != 0x02.toByte() || manufacturerData[1] != 0x15.toByte()) return

            val uuidBytes = ByteArray(16)
            System.arraycopy(manufacturerData, 2, uuidBytes, 0, 16)
            val scannedUuid = bytesToHex(uuidBytes)

            if (scannedUuid != TARGET_IBEACON_UUID) return

            val major = ((manufacturerData[18].toInt() and 0xFF) shl 8) or (manufacturerData[19].toInt() and 0xFF)
            val minor = ((manufacturerData[20].toInt() and 0xFF) shl 8) or (manufacturerData[21].toInt() and 0xFF)
            val txPower = manufacturerData[22].toInt()

            if (!kalmanFilters.containsKey(address)) {
                kalmanFilters[address] = KalmanFilter()
            }
            val smoothRssi = kalmanFilters[address]!!.filter(rssi.toDouble())

            val currentTime = System.currentTimeMillis()
            val lastUpdateTime = lastUpdateMap[address] ?: 0L
            if (currentTime - lastUpdateTime < 1000) return
            lastUpdateMap[address] = currentTime

            val distance = calculateDistance(smoothRssi, txPower)

            val beaconItem = BeaconItem(
                macAddress = address,
                uuid = scannedUuid,
                major = major,
                minor = minor,
                rssi = rssi,
                txPower = txPower,
                estimatedDistance = distance,
                type = "iBeacon"
            )

            // Bulunan veriyi MainActivity'ye gönder
            listener.onBeaconFound(beaconItem)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            println("Tarama Hatası: $errorCode")
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        val raw = sb.toString()
        if (raw.length == 32) {
            return "${raw.substring(0, 8)}-${raw.substring(8, 12)}-${raw.substring(12, 16)}-${raw.substring(16, 20)}-${raw.substring(20, 32)}"
        }
        return raw
    }

    private fun calculateDistance(rssi: Double, txPower: Int): Double {
        if (rssi == 0.0) return -1.0
        val envFactor = 2.0
        return 10.0.pow((txPower - rssi) / (10 * envFactor))
    }
}