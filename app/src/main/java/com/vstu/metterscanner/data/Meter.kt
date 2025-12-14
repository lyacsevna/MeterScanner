package com.vstu.metterscanner.data

import com.vstu.metterscanner.data.local.MeterEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Meter(
    val id: Long = 0,
    val type: MeterType,
    val value: Double,
    val date: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
    val note: String = "",
    val photoPath: String? = null
) {
    fun toEntity(): MeterEntity {
        return MeterEntity(
            id = id,
            type = type,
            value = value,
            date = date,
            note = note,
            photoPath = photoPath
        )
    }

    companion object {
        fun fromEntity(entity: MeterEntity): Meter {
            return Meter(
                id = entity.id,
                type = entity.type,
                value = entity.value,
                date = entity.date,
                note = entity.note,
                photoPath = entity.photoPath
            )
        }
    }
}