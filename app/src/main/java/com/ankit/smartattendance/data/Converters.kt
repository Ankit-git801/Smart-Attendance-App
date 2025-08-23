package com.ankit.smartattendance.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromRecordType(value: RecordType): String {
        return value.name
    }

    @TypeConverter
    fun toRecordType(value: String): RecordType {
        return RecordType.valueOf(value)
    }
}
