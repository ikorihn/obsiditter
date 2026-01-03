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

    fun toEntry(): String {
        return buildString {
            append("- $time")
            if (content.isNotBlank()) {
                append(" ")
                append(content.replace("\n", "\n    "))
            }
        }
    }

}

fun notesToEntry(notes: List<Note>): String {
    return notes.sortedBy { it.datetime }
        .joinToString("\n") { it.toEntry() }
}