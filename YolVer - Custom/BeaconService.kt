package com.example.yolver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BeaconService : Service(), BeaconListener {

    private var beaconScanner: BeaconScanner? = null
    private val channelid = "AmbulanceScannerChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Kullanıcıya kalıcı bildirimi göster ve servisi ölümsüz yap
        val notification: Notification = NotificationCompat.Builder(this, channelid)
            .setContentTitle("YolVer Aktif")
            .setContentText("Ambulanslar için çevre taranıyor...")
            .setSmallIcon(R.mipmap.ic_launcher_round) // Kendi ikonunuzu buraya koyun
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)

        // Tarayıcıyı Başlat
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val scanner = bluetoothManager.adapter?.bluetoothLeScanner

        beaconScanner = BeaconScanner(scanner, this)
        beaconScanner?.startScanning()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Servis sistem tarafından öldürülürse hemen yeniden başlat (START_STICKY)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Binding kullanmayacağız, standart servis
    }

    // BeaconScanner'dan veri geldiğinde burası tetiklenecek
    override fun onBeaconFound(beacon: BeaconItem) {
        val broadcastIntent = Intent("AMBULANCE_FOUND_ACTION")

        // "Bu yayını sadece kendi paketimin içine gönder"
        broadcastIntent.setPackage(applicationContext.packageName)

        broadcastIntent.putExtra("macAddress", beacon.macAddress)
        broadcastIntent.putExtra("distance", beacon.estimatedDistance)
        broadcastIntent.putExtra("rssi", beacon.rssi)
        broadcastIntent.putExtra("smoothRssi", beacon.smoothRssi)
        sendBroadcast(broadcastIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconScanner?.stopScanning()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelid,
                "Ambulans Tarama Servisi",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}