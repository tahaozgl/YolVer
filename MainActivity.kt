package com.example.viyo

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// MainActivity, BeaconListener arayüzünü uyguluyor (implement ediyor)
class MainActivity : AppCompatActivity(), BeaconListener {

    private lateinit var beaconAdapter: BeaconAdapter
    private var beaconScanner: BeaconScanner? = null
    private val permissionRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI Hazırlıkları
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        beaconAdapter = BeaconAdapter()
        recyclerView.adapter = beaconAdapter

        checkPermissionsAndStartScan()
    }

    private fun checkPermissionsAndStartScan() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, permissionRequestCode)
        } else {
            initializeAndStartScanner()
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
                initializeAndStartScanner()
            } else {
                Toast.makeText(this, "İzin reddedildi, tarama yapılamıyor.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeAndStartScanner() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        // Scanner sınıfımızı oluşturuyoruz ve "this" diyerek Listener olarak bu Activity'yi veriyoruz
        beaconScanner = BeaconScanner(bluetoothLeScanner, this)

        Toast.makeText(this, "Hedef iBeacon Taranıyor...", Toast.LENGTH_SHORT).show()
        beaconScanner?.startScanning()
    }

    // BeaconScanner'dan gelen veriler bu fonksiyona düşecek
    override fun onBeaconFound(beacon: BeaconItem) {
        // Gelen veriyi RecyclerView listemize ekliyoruz
        beaconAdapter.updateList(beacon)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Uygulama kapanırken taramayı durdurmayı unutmayalım (Pil tasarrufu için önemli)
        beaconScanner?.stopScanning()
    }
}