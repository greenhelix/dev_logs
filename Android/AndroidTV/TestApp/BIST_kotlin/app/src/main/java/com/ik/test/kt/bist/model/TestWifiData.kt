package com.ik.test.kt.bist.model

data class TestWifiData(
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val pingMs: Int,
    val testLocation: String,
    val testedAt: Long
)