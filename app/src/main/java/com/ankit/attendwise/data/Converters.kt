package com.ankit.attendwise.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromRecordType(value: RecordType?): String {
        return value?.name ?: RecordType.CLASS.name
    }

    @TypeConverter
    fun toRecordType(value: String?): RecordType {
        if (value.isNullOrBlank()) return RecordType.CLASS
        return try {
            RecordType.valueOf(value.trim().uppercase())
        } catch (e: Exception) {
            RecordType.CLASS
        }
    }
}
