package com.example.yolver

data class BeaconItem(
    val macAddress: String,
    val uuid: String,
    val major: Int,
    val minor: Int,
    var rssi: Int,
    var smoothRssi: Double,      // Filtrelenmiş (Pürüzsüz) RSSI -> TEST İÇİN EKLENDİ
    var txPower: Int = -47,
    var estimatedDistance: Double,
    var type: String
)