package com.pw999.lnxsense.android.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DialConfig(
    val slotIndex: Int,
    val deviceId: String,
    val sensorId: String,
    val deviceName: String,
    val sensorName: String,
    val unit: String,
    val format: String,
    val minValue: Double,
    val maxValue: Double
)
