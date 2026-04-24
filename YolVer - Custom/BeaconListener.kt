package com.example.yolver

interface BeaconListener {
    // Yeni bir beacon bulunduğunda veya güncellendiğinde tetiklenecek
    fun onBeaconFound(beacon: BeaconItem)
}