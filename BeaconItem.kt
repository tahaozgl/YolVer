package com.example.viyo

data class BeaconItem(
    val macAddress: String,
    val uuid: String,
    val major: Int,
    val minor: Int,
    var rssi: Int,
    var txPower: Int,
    var estimatedDistance: Double,
    var type: String
)