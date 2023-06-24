package com.yjy.forestory.model.db

import android.net.Uri
import androidx.room.TypeConverter
import java.util.*

class Converter {

    // Uri(실사용) <-> String(DB) 변환
    @TypeConverter
    fun fromUri(uri: Uri): String {
        return uri.toString()
    }
    @TypeConverter
    fun toUri(uriString: String): Uri {
        return Uri.parse(uriString)
    }

    // Data(실사용) <-> Long(DB) 변환
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}