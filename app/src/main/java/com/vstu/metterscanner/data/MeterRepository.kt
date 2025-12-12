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

    suspend fun updateMeter(meter: Meter) {
        meterDao.update(meter.toEntity())
    }

    suspend fun deleteMeter(meter: Meter) {
        meterDao.delete(meter.toEntity())
    }

    suspend fun getMeterById(id: Long): Meter? {
        return meterDao.getById(id)?.let { Meter.fromEntity(it) }
    }

    suspend fun getAllMetersSync(): List<Meter> {
        return allMeters.first()
    }

    fun getMetersByType(type: MeterType): Flow<List<Meter>> {
        return meterDao.getMetersByType(type)
            .map { entities -> entities.map { Meter.fromEntity(it) } }
    }

    suspend fun getLastMeter(type: MeterType): Meter? {
        return meterDao.getLastMeter(type)?.let { Meter.fromEntity(it) }
    }

    private suspend fun <T> Flow<T>.first(): T {
        var result: T? = null
        collect { value ->
            result = value
            return@collect
        }
        return result!!
    }
}