package com.example.viyo

class KalmanFilter {
    private var r = 1.0
    private var q = 0.1
    private var a = 1.0
    private var b = 0.0
    private var c = 1.0
    private var cov = Double.NaN
    private var x = Double.NaN

    fun filter(measurement: Double): Double {
        if (x.isNaN()) {
            x = measurement
            cov = 1.0
        } else {
            val predX = (a * x) + (b * 0)
            val predCov = (a * cov * a) + r
            val k = predCov * c * (1 / ((c * predCov * c) + q))
            x = predX + k * (measurement - (c * predX))
            cov = predCov - (k * c * predCov)
        }
        return x
    }
}