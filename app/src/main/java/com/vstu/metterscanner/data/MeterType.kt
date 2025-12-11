package com.vstu.metterscanner.data

enum class MeterType {
    ELECTRICITY,
    COLD_WATER,
    HOT_WATER;

    companion object {
        fun fromString(value: String): MeterType {
            return when (value) {
                "ELECTRICITY" -> ELECTRICITY
                "COLD_WATER" -> COLD_WATER
                "HOT_WATER" -> HOT_WATER
                else -> ELECTRICITY
            }
        }
    }
}