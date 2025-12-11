package com.vstu.metterscanner.data.local

import androidx.room.TypeConverter
import com.vstu.metterscanner.data.MeterType

class Converters {
    @TypeConverter
    fun fromMeterType(type: MeterType): String {
        return type.name
    }

    @TypeConverter
    fun toMeterType(value: String): MeterType {
        return MeterType.valueOf(value)
    }
}