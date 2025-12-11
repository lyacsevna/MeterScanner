package com.vstu.metterscanner.data

import android.content.Context
import com.vstu.metterscanner.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MeterRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val meterDao = database.meterDao()

    val allMeters: Flow<List<Meter>> =
        meterDao.getAllMeters()
            .map { entities -> entities.map { Meter.fromEntity(it) } }

    suspend fun addMeter(meter: Meter): Long {
        return meterDao.insert(meter.toEntity())
    }

    suspend fun getAllMetersSync(): List<Meter> {
        val entities = meterDao.getAllMeters()
        // Для синхронного доступа можно использовать Flow.first()
        return emptyList() // Временное решение
    }

    fun getMetersByType(type: MeterType): Flow<List<Meter>> {
        return meterDao.getMetersByType(type)
            .map { entities -> entities.map { Meter.fromEntity(it) } }
    }

    suspend fun getLastMeter(type: MeterType): Meter? {
        return meterDao.getLastMeter(type)?.let { Meter.fromEntity(it) }
    }
}