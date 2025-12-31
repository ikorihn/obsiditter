package com.ikorihn.obsiditter.data

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.ikorihn.obsiditter.model.Note
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NoteRepository(private val context: Context) {

    private val prefs = Prefs(context)

    private fun getRootDirectory(): DocumentFile? {
        val uri = prefs.storageUri ?: return null
        return try {
            DocumentFile.fromTreeUri(context, uri)
        } catch (e: Exception) {
            null
        }
    }

    fun getAllNotes(): List<Note> {
        val dir = getRootDirectory() ?: return emptyList()
        val files =
            dir.listFiles().filter { it.name?.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.md")) == true }

        return files.sortedByDescending { it.name }.flatMap { file ->
            parseFile(file)
        }
    }

    fun getNotesForDate(date: String): List<Note> {
        val dir = getRootDirectory() ?: return emptyList()
        val file = dir.findFile("$date.md") ?: return emptyList()
        return parseFile(file)
    }

    private fun parseFile(file: DocumentFile): List<Note> {
        val dateStr = file.name?.removeSuffix(".md") ?: return emptyList()
        val content = readText(file) ?: return emptyList()
        val lines = content.lines()

        val notes = mutableListOf<Note>()
        var inJournal = false
        var currentNoteTime: String? = null
        var currentNoteContent = StringBuilder()

        for (line in lines) {
            if (line.trim() == "## Journal") {
                inJournal = true
                continue
            }
            if (!inJournal) continue

            // Check for list item with time "- HH:mm"
            // Regex: ^- (\\d{2}:\\d{2})\\s*(.*)
            val match = Regex("^-\\s+(\\d{2}:\\d{2})\\s*(.*)").find(line)
            if (match != null) {
                // Save previous note if exists
                if (currentNoteTime != null) {
                    notes.add(Note(dateStr, currentNoteTime, currentNoteContent.toString().trim()))
                }
                // Start new note
                currentNoteTime = match.groupValues[1]
                currentNoteContent = StringBuilder()
                if (match.groupValues[2].isNotBlank()) {
                    currentNoteContent.append(match.groupValues[2]).append("\n")
                }
            } else if (currentNoteTime != null) {
                // Append to current note
                currentNoteContent.append(line).append("\n")
            }
        }
        // Add last note
        if (currentNoteTime != null) {
            notes.add(Note(dateStr, currentNoteTime, currentNoteContent.toString().trim()))
        }
        return notes
    }

    fun addNote(note: Note) {
        val dir = getRootDirectory() ?: return
        var file = dir.findFile("${note.date}.md")

        if (file == null) {
            file = dir.createFile("text/markdown", "${note.date}.md")
        }
        if (file != null) {
            createFileContent(file, note.date)
        }

        if (file == null) return

        // Append to ## Journal
        val content = readText(file) ?: ""
        val journalHeader = "## Journal"

        if (!content.contains(journalHeader)) {
            appendText(file, "\n\n$journalHeader\n")
        }

        val noteEntry = buildString {
            append("\n- ")
            append(note.time)
            if (note.content.isNotBlank()) {
                append("\n    ")
                append(note.content.replace("\n", "\n    "))
            }
        }

        appendText(file, noteEntry)
    }

    fun updateNote(date: String, index: Int, newContent: String) {
        val dir = getRootDirectory() ?: return
        val file = dir.findFile("$date.md") ?: return

        val notes = parseFile(file).toMutableList()
        if (index in notes.indices) {
            val oldNote = notes[index]
            notes[index] = oldNote.copy(content = newContent)
            rewriteFile(file, notes)
        }
    }

    fun deleteNote(date: String, index: Int) {
        val dir = getRootDirectory() ?: return
        val file = dir.findFile("$date.md") ?: return

        val notes = parseFile(file).toMutableList()
        if (index in notes.indices) {
            notes.removeAt(index)
            rewriteFile(file, notes)
        }
    }

    private fun createFileContent(file: DocumentFile, date: String) {
        val now = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + "+09:00"

        val text = """
---
date: "$now"
tags: 
    - 'daily'
fileClass: DailyLog
mood_morning:
wake_time:
mood_evening:
sleep_time:
snacks:
reading_min:
exercise_min:
---

## Memo

## Journal
""".trimIndent()
        writeText(file, text)
    }

    private fun rewriteFile(file: DocumentFile, notes: List<Note>) {
        val content = readText(file) ?: return
        val lines = content.lines()
        val sb = StringBuilder()
        var inJournal = false

        for (line in lines) {
            if (line.trim() == "## Journal") {
                sb.append(line).append("\n")
                inJournal = true
                break
            }
            sb.append(line).append("\n")
        }

        if (!inJournal) {
            sb.append("\n## Journal\n")
        }

        for (note in notes) {
            sb.append("\n- ")
            sb.append(note.time)
            if (note.content.isNotBlank()) {
                sb.append("\n    ")
                sb.append(note.content.replace("\n", "\n    "))
            }
        }
        writeText(file, sb.toString())
    }

    private fun readText(file: DocumentFile): String? {
        return try {
            context.contentResolver.openInputStream(file.uri)?.use {
                BufferedReader(InputStreamReader(it)).readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeText(file: DocumentFile, text: String) {
        try {
            context.contentResolver.openOutputStream(file.uri, "w")?.use {
                it.write(text.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun appendText(file: DocumentFile, text: String) {
        try {
            context.contentResolver.openOutputStream(file.uri, "wa")?.use {
                it.write(text.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isStorageConfigured(): Boolean {
        return prefs.storageUri != null
    }
}