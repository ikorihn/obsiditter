package com.ikorihn.obsiditter.model

data class Note(
    val date: String, // yyyy-MM-dd
    val time: String, // HH:mm
    val content: String,
    val tags: List<String> = emptyList()
)
