package com.example.yolver

import java.util.LinkedList

class HybridSignalFilter(private val windowSize: Int = 5) {

    // Medyan filtresi için son verileri tutacağımız pencere
    private val rssiWindow = LinkedList<Double>()

    // Kalman Filtresi Değişkenleri
    private var r = 2.0  // Ölçüm Gürültüsü (BLE için biraz daha yüksek tutulur)
    private var q = 0.1  // Süreç Gürültüsü (Ambulans hareketli olduğu için çok düşük olmamalı)
    private var cov = Double.NaN
    private var x = Double.NaN

    fun filter(newRssi: Double): Double {
        // --- 1. AŞAMA: MEDYAN FİLTRESİ (Uç Değerleri Budama) ---
        rssiWindow.addLast(newRssi)

        // Pencere boyutunu (örneğin 5) aşarsak en eski veriyi sil
        if (rssiWindow.size > windowSize) {
            rssiWindow.removeFirst()
        }

        val medianRssi: Double
        if (rssiWindow.size < 3) {
            // Yeterli veri yoksa ham veriyi kullan
            medianRssi = newRssi
        } else {
            // Verileri küçükten büyüğe sırala ve tam ortadakini al (Medyan)
            val sortedWindow = rssiWindow.sorted()
            medianRssi = sortedWindow[sortedWindow.size / 2]
        }

        // --- 2. AŞAMA: KALMAN FİLTRESİ (Pürüzsüzleştirme) ---
        if (x.isNaN()) {
            x = medianRssi
            cov = 1.0
        } else {
            // Tahmin (Prediction)
            val predX = x
            val predCov = cov + q

            // Düzeltme (Update)
            val k = predCov / (predCov + r) // Kalman Kazancı
            x = predX + k * (medianRssi - predX)
            cov = (1 - k) * predCov
        }

        // Temizlenmiş ve pürüzsüzleştirilmiş nihai RSSI değerini döndür
        return x
    }
}