package com.ikorihn.obsiditter.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Note(
    val date: String, // yyyy-MM-dd
    val time: String, // HH:mm
    val content: String,
    val tags: List<String> = emptyList()

) {

    val datetime: LocalDateTime
        get() {
            return LocalDateTime.parse(
                "$date $time",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            )
        }

}