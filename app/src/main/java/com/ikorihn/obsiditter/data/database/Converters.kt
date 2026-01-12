package com.ikorihn.obsiditter.data.database

import androidx.room.TypeConverter
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class Converters {
    private val mapper = jacksonObjectMapper()

    @TypeConverter
    fun fromStringMap(value: String): Map<String, Any> {
        return try {
            mapper.readValue(value, object : TypeReference<Map<String, Any>>() {})
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromMap(map: Map<String, Any>): String {
        return try {
            mapper.writeValueAsString(map)
        } catch (e: Exception) {
            "{}"
        }
    }
}
