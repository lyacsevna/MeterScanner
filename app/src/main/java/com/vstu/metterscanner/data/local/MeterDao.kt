package com.vstu.metterscanner.data.local

import androidx.room.*
import com.vstu.metterscanner.data.MeterType
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterDao {
    @Insert
    suspend fun insert(meter: MeterEntity): Long

    @Query("SELECT * FROM meters ORDER BY date DESC")
    fun getAllMeters(): Flow<List<MeterEntity>>

    @Query("SELECT * FROM meters WHERE type = :type ORDER BY date DESC")
    fun getMetersByType(type: MeterType): Flow<List<MeterEntity>>

    @Query("SELECT * FROM meters WHERE type = :type ORDER BY date DESC LIMIT 1")
    suspend fun getLastMeter(type: MeterType): MeterEntity?
}