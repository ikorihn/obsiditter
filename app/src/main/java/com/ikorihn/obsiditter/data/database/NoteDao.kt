package com.ikorihn.obsiditter.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE category = :categoryId ORDER BY filename DESC") // Sort by filename ~ date usually
    suspend fun getNotesByCategory(categoryId: String): List<NoteEntity>

    @Query("SELECT filename, lastModified FROM notes WHERE category = :categoryId")
    suspend fun getNoteMetadatas(categoryId: String): List<NoteMetadataTuple>

    @Upsert
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE category = :categoryId AND filename = :filename")
    suspend fun deleteNote(categoryId: String, filename: String)

    @Query("DELETE FROM notes WHERE category = :categoryId AND filename NOT IN (:filenames)")
    suspend fun deleteMissingNotes(categoryId: String, filenames: List<String>)
    
    @Query("DELETE FROM notes WHERE category = :categoryId")
    suspend fun deleteAllByCategory(categoryId: String)
}

data class NoteMetadataTuple(
    val filename: String,
    val lastModified: Long
)
