package com.vstu.metterscanner.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vstu.metterscanner.data.MeterType

@Entity(tableName = "meters")
data class MeterEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "type")
    val type: MeterType,

    @ColumnInfo(name = "value")
    val value: Double,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "note")
    val note: String = ""
)