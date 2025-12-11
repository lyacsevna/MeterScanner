package com.vstu.metterscanner.data

import java.time.LocalDateTime

data class Meter(
    val id: Long? = null,
    val type: MeterType,
    val value: Double,
    val date: LocalDateTime = LocalDateTime.now(),
    val note: String = ""
)