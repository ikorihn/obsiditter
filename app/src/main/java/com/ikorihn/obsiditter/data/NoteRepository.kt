package com.ikorihn.obsiditter.data

import android.content.Context
import com.ikorihn.obsiditter.model.Note
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NoteRepository(private val context: Context) {

    private fun getDirectory(): File {
        // storage/emulated/0/Android/data/com.ikorihn.obsiditter/files/Journal
        val dir = File(context.getExternalFilesDir(null), "Journal")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAllNotes(): List<Note> {
        val dir = getDirectory()
        val files = dir.listFiles { _, name -> name.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.md")) }
            ?: return emptyList()

        return files.sortedDescending().flatMap {
            parseFile(it)
        }
    }

    fun getNotesForDate(date: String): List<Note> {
        val file = File(getDirectory(), "$date.md")
        if (!file.exists()) return emptyList()
        return parseFile(file)
    }

    private fun parseFile(file: File): List<Note> {
        val dateStr = file.nameWithoutExtension
        val lines = file.readLines()
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
            // Regex: ^- (\d{2}:\d{2})\s*(.*)
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
        val file = File(getDirectory(), "${note.date}.md")
        if (!file.exists()) {
            createFile(file, note.date)
        }

        // Append to ## Journal
        // We need to insert it in the correct place? Or just append? 
        // Spec says "append under ## Journal with timestamp".
        // Appending is easiest.
        
        val content = file.readText()
        val journalHeader = "## Journal"
        
        if (!content.contains(journalHeader)) {
            // Append header if missing (should not happen if created correctly)
            file.appendText("\n\n$journalHeader\n")
        }

        val noteEntry = buildString {
            append("\n- ")
            append(note.time)
            if (note.content.isNotBlank()) {
                append("\n    ")
                append(note.content.replace("\n", "\n    "))
            }
        }
        
        // Naive append: just append to end of file.
        // Assuming ## Journal is the last section or we just append to it.
        // If there are other sections after Journal, this might break.
        // Spec only mentions ## Memo and ## Journal.
        // Let's assume ## Journal is likely at the end or we can just append.
        
        file.appendText(noteEntry)
    }

    fun updateNote(date: String, index: Int, newContent: String) {
        val file = File(getDirectory(), "$date.md")
        if (!file.exists()) return

        val notes = parseFile(file).toMutableList()
        if (index in notes.indices) {
            val oldNote = notes[index]
            notes[index] = oldNote.copy(content = newContent)
            rewriteFile(file, notes)
        }
    }

    fun deleteNote(date: String, index: Int) {
        val file = File(getDirectory(), "$date.md")
        if (!file.exists()) return

        val notes = parseFile(file).toMutableList()
        if (index in notes.indices) {
            notes.removeAt(index)
            rewriteFile(file, notes)
        }
    }

    private fun createFile(file: File, date: String) {
        // Create frontmatter
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + "+09:00" // Hardcoded timezone for now or use system? Spec has +09:00.
        // I'll use system offset but formatted similarly if possible, or just strict spec example.
        // Spec: date: "2025-12-29T12:39:00+09:00"
        
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
        file.writeText(text)
    }

    private fun rewriteFile(file: File, notes: List<Note>) {
        // Read file up to ## Journal
        val lines = file.readLines()
        val sb = StringBuilder()
        var inJournal = false
        
        for (line in lines) {
            if (line.trim() == "## Journal") {
                sb.append(line).append("\n")
                inJournal = true
                break // Stop reading original file content for journal part
            }
            sb.append(line).append("\n")
        }
        
        if (!inJournal) {
             sb.append("\n## Journal\n")
        }

        // Write notes
        for (note in notes) {
            sb.append("\n- ")
            sb.append(note.time)
            if (note.content.isNotBlank()) {
                sb.append("\n    ")
                sb.append(note.content.replace("\n", "\n    "))
            }
        }
        
        file.writeText(sb.toString())
    }
}
