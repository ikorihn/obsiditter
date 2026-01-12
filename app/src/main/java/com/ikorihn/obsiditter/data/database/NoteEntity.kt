package com.ikorihn.obsiditter.data.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "notes",
    primaryKeys = ["category", "filename"],
    indices = [Index(value = ["category"])]
)
data class NoteEntity(
    val category: String,
    val filename: String,
    val lastModified: Long,
    val frontmatter: Map<String, Any>,
    val bodySnippet: String
)
