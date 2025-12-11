package com.vstu.metterscanner.data

class MeterRepository {
    private val meters = mutableListOf<Meter>()
    private var nextId = 1L

    fun addMeter(meter: Meter): Long {
        val newMeter = meter.copy(id = nextId++)
        meters.add(newMeter)
        return newMeter.id ?: -1
    }

    fun getAllMeters(): List<Meter> = meters.sortedByDescending { it.date }

    fun getMetersByType(type: MeterType): List<Meter> =
        meters.filter { it.type == type }.sortedByDescending { it.date }

    fun getLastMeter(type: MeterType): Meter? =
        meters.filter { it.type == type }.maxByOrNull { it.date }
}