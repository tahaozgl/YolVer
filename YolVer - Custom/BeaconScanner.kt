@file:Suppress("SameParameterValue")

package com.example.yolver

import android.annotation.SuppressLint
import android.bluetooth.le.*
import kotlin.math.pow

class BeaconScanner(
    private val scanner: BluetoothLeScanner?,
    private val listener: BeaconListener
) {
    private val signalFilters = HashMap<String, HybridSignalFilter>()
    private val lastUpdateMap = HashMap<String, Long>()

    // Şirket ID'si (0xFFFF = 65535)
    private val targetCompanyId = 65535

    // Ambulansın yayınladığı özel veri (0x30, 0x37, 0x41, 0x42, 0x43, 0x30, 0x37)
    // Bu aslında ASCII olarak "07ABC07" plakasıdır.
    private val targetPayloadHex = "30374142433037"

    companion object {
        const val DEFAULT_TX_POWER = -47
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (scanner == null) return

        // İyileştirme 1: Android sisteminin pili tüketmemesi için sadece bizim
        // Şirket ID'mize (0xFFFF) sahip cihazları filtreleyerek tarama başlatıyoruz.
        val filter = ScanFilter.Builder()
            .setManufacturerData(targetCompanyId, byteArrayOf())
            .build()
        val filters: MutableList<ScanFilter> = ArrayList()
        filters.add(filter)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Hızlı tepki için
            .build()

        scanner.startScan(filters, settings, scanCallback)
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

            // Sadece 0xFFFF (65535) ID'si ile gelen özel veriyi al
            val manufacturerData = scanRecord.getManufacturerSpecificData(targetCompanyId) ?: return

            // Byte dizisini Hex String'e çevirip kontrol edelim
            val scannedPayloadHex = bytesToHex(manufacturerData)

            // Eğer gelen veri bizim ambulansın verisi değilse yoksay
            if (scannedPayloadHex != targetPayloadHex) return

            // Kalman filtresi ile sinyal dalgalanmalarını yumuşat
            if (!signalFilters.containsKey(address)) {
                // windowSize=5 demek, son 5 verinin medyanını al demektir. (Tepki hızı ve stabilite için idealdir)
                signalFilters[address] = HybridSignalFilter(windowSize = 5)
            }
            // Ham RSSI'ı önce Medyan'a sonra Kalman'a sokup pürüzsüz halini alıyoruz
            val smoothRssi = signalFilters[address]!!.filter(rssi.toDouble())

            // Saniyede ~3-4 kere UI güncellemesi yapmak için (Okunabilirlik için ideal)
            val currentTime = System.currentTimeMillis()
            val lastUpdateTime = lastUpdateMap[address] ?: 0L
            // 300 milisaniyeden (0.3 saniye) kısaysa güncellemeyi atla
            if (currentTime - lastUpdateTime < 300) return
            lastUpdateMap[address] = currentTime

            val txPower = DEFAULT_TX_POWER
            val distance = calculateDistance(smoothRssi, txPower)

            val beaconItem = BeaconItem(
                macAddress = address,
                uuid = scannedPayloadHex, // Artık UUID yerine Payload gösteriyoruz
                major = 0, // Özel paket olduğu için bunlar yok
                minor = 0, // Özel paket olduğu için bunlar yok
                rssi = rssi, // Ekranda gerçek değeri göster
                smoothRssi = smoothRssi, // Filtreli veriyi de yolluyoruz
                txPower = txPower,
                estimatedDistance = distance,
                type = "Ambulance"
            )

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
        return sb.toString()
    }

    private fun calculateDistance(rssi: Double, txPower: Int): Double {
        if (rssi == 0.0) return -1.0
        // Çevre faktörü (2.0 ile 4.0 arası). Araç/Dış ortam için 3.0 daha gerçekçidir.
        val envFactor = 2.2
        return 10.0.pow((txPower - rssi) / (10 * envFactor))
    }
}