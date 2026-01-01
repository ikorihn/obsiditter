package com.ikorihn.obsiditter.data

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.ikorihn.obsiditter.model.Note
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import java.io.BufferedReader
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

        return files.sortedByDescending { it.name }.flatMap {
            parseFile(it)
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

        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(content)

        val notes = mutableListOf<Note>()
        var foundJournal = false

        for (node in parsedTree.children) {
            if (node.type.name == "ATX_2") {
                val headerText = node.getTextInNode(content).toString().trim()
                if (headerText.endsWith("Journal")) {
                    foundJournal = true
                    continue
                } else if (foundJournal) {
                    // Next header found, stop
                    break
                }
            }

            if (foundJournal && node.type.name == "UNORDERED_LIST") {
                for (item in node.children) {
                    if (item.type.name == "LIST_ITEM") {
                        val itemText = item.getTextInNode(content).toString()
                        
                        val lines = itemText.lines()
                        if (lines.isEmpty()) continue
                        
                        val firstLine = lines[0].trim()
                        val match = Regex("^[-*+]\\s+(\\d{2}:\\[d{2})\\s*(.*)").find(firstLine)
                        
                        if (match != null) {
                            val time = match.groupValues[1]
                            val firstLineContent = match.groupValues[2]
                            
                            val contentBuilder = StringBuilder()
                            if (firstLineContent.isNotBlank()) {
                                contentBuilder.append(firstLineContent)
                            }
                            
                            for (i in 1 until lines.size) {
                                contentBuilder.append("\n").append(lines[i].trim())
                            }
                            
                            notes.add(Note(dateStr, time, contentBuilder.toString().trim()))
                        }
                    }
                }
            }
        }
        return notes
    }

    fun addNote(note: Note) {
        val dir = getRootDirectory() ?: return
        var file = dir.findFile("${note.date}.md")

        if (file == null) {
            file = dir.createFile("text/markdown", "${note.date}.md")
            if (file != null) {
                 createFileContent(file, note.date)
            }
        }
        
        if (file == null) return

        val content = readText(file) ?: ""
        if (content.isEmpty() && file.length() == 0L) {
             createFileContent(file, note.date)
        }
        
        // Re-read content in case it was created
        val currentContent = readText(file) ?: ""
        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(currentContent)

        var foundJournal = false
        
        for (node in parsedTree.children) {
            if (node.type.name == "ATX_2") {
                val text = node.getTextInNode(currentContent).toString().trim()
                if (text.endsWith("Journal")) {
                    foundJournal = true
                    break
                }
            }
        }

        val noteEntry = buildString {
            append("- ")
            append(note.time)
            if (note.content.isNotBlank()) {
                append("\n    ")
                append(note.content.replace("\n", "\n    "))
            }
            append("\n")
        }

        if (!foundJournal) {
            val toAppend = "\n\n## Journal\n$noteEntry"
            appendText(file, toAppend)
        } else {
             // For now, simpler to append to end. 
             // If robustness is needed for insertion in middle, logic would be complex here.
             // Given the spec implies Journal is the main appended section.
             val prefix = if (currentContent.endsWith("\n")) "" else "\n"
             appendText(file, prefix + noteEntry)
        }
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
        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(content)
        
        var journalStartOffset = -1
        var journalEndOffset = -1
        var foundJournal = false
        
        for (node in parsedTree.children) {
            if (node.type.name == "ATX_2") {
                val text = node.getTextInNode(content).toString().trim()
                if (text.endsWith("Journal")) {
                    foundJournal = true
                    journalStartOffset = node.startOffset
                    journalEndOffset = node.endOffset 
                    continue
                } else if (foundJournal) {
                    journalEndOffset = node.startOffset
                    break
                }
            }
            if (foundJournal) {
                 journalEndOffset = node.endOffset
            }
        }
        
        if (!foundJournal) {
             val sb = StringBuilder(content)
             sb.append("\n\n## Journal\n")
             for (note in notes) {
                appendNoteToStringBuilder(sb, note)
             }
             writeText(file, sb.toString())
             return
        }
        
        val sb = StringBuilder()
        sb.append(content.substring(0, journalStartOffset))
        sb.append("## Journal\n")
        
        for (note in notes) {
            appendNoteToStringBuilder(sb, note)
        }
        
        if (journalEndOffset < content.length) {
             val rest = content.substring(journalEndOffset)
             if (rest.isNotBlank() && !rest.startsWith("\n")) sb.append("\n")
             sb.append(rest)
        }
        
        writeText(file, sb.toString())
    }
    
    private fun appendNoteToStringBuilder(sb: StringBuilder, note: Note) {
        sb.append("- ")
        sb.append(note.time)
        if (note.content.isNotBlank()) {
            sb.append("\n    ")
            sb.append(note.content.replace("\n", "\n    "))
        }
        sb.append("\n")
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
