package com.example.yolver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var beaconAdapter: BeaconAdapter
    private val permissionRequestCode = 1

    // Servisten gelecek haberleri dinleyen radyo alıcımız
    private val ambulanceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "AMBULANCE_FOUND_ACTION") {
                val mac = intent.getStringExtra("macAddress") ?: return
                val distance = intent.getDoubleExtra("distance", -1.0)
                val rssi = intent.getIntExtra("rssi", 0)
                val smoothRssi = intent.getDoubleExtra("smoothRssi", 0.0)

                val beacon = BeaconItem(
                    macAddress = mac,
                    uuid = "30374142433037",
                    major = 0, minor = 0, rssi = rssi, smoothRssi = smoothRssi, txPower = -47,
                    estimatedDistance = distance, type = "Ambulance"
                )

                // Arayüzü güncelle
                beaconAdapter.updateList(beacon)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        beaconAdapter = BeaconAdapter()
        recyclerView.adapter = beaconAdapter

        checkPermissionsAndStartService()
    }

    override fun onResume() {
        super.onResume()
        // Ekran açıldığında servisten gelen mesajları dinlemeye başla
        val filter = IntentFilter("AMBULANCE_FOUND_ACTION")

        ContextCompat.registerReceiver(
            this,
            ambulanceReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        // Ekran kapandığında dinlemeyi bırak (Pil tasarrufu için, ama servis arka planda taramaya devam eder!)
        unregisterReceiver(ambulanceReceiver)
    }

    private fun checkPermissionsAndStartService() {
        // İzinleri dinamik bir listeye koyuyoruz ki sürüme göre ekleme yapabilelim
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 ve üzeri Bluetooth izinleri
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Android 11 ve altı zorunlu konum ve Bluetooth izinleri
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // ANDROID 13 VE ÜZERİ İÇİN BİLDİRİM İZNİ EKLENİYOR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsArray = permissionsToRequest.toTypedArray()

        if (!hasPermissions(permissionsArray)) {
            ActivityCompat.requestPermissions(this, permissionsArray, permissionRequestCode)
        } else {
            startAmbulanceService()
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (perm in permissions) {
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startAmbulanceService()
            } else {
                Toast.makeText(this, "İzin reddedildi, sistem çalışamaz.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startAmbulanceService() {
        val serviceIntent = Intent(this, BeaconService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        Toast.makeText(this, "Sistem Aktif: Arka planda çalışıyor.", Toast.LENGTH_SHORT).show()
    }
}