package com.example.viyo

interface BeaconListener {
    // Yeni bir beacon bulunduğunda veya güncellendiğinde tetiklenecek
    fun onBeaconFound(beacon: BeaconItem)
}