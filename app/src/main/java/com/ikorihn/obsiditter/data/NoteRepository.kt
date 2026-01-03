package com.ikorihn.obsiditter.data

import android.content.Context
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.ikorihn.obsiditter.model.Note
import com.ikorihn.obsiditter.model.notesToEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


data class NoteFile(
    val name: String,
    val file: DocumentFile
)

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

    suspend fun getSortedNoteFiles(): List<NoteFile> = withContext(Dispatchers.IO) {
        val uri = prefs.storageUri ?: return@withContext emptyList()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        val sortOrder = "${DocumentsContract.Document.COLUMN_DISPLAY_NAME} DESC"

        val result = mutableListOf<NoteFile>()
        val regex = Regex("\\d{4}-\\d{2}-\\d{2}\\.md")

        try {
            context.contentResolver.query(
                childrenUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex =
                    cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex) ?: continue

                    if (regex.matches(name)) {
                        val docId = cursor.getString(idIndex)
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                        val file = DocumentFile.fromSingleUri(context, docUri)
                        if (file != null) {
                            result.add(NoteFile(name, file))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext getRootDirectory()?.listFiles()
                ?.filter { it.name?.matches(regex) == true }
                ?.sortedByDescending { it.name }
                ?.mapNotNull { file ->
                    file.name?.let { NoteFile(it, file) }
                }
                ?: emptyList()
        }

        result.sortByDescending { it.name }

        return@withContext result
    }

    suspend fun parseNotes(files: List<NoteFile>): List<Note> = withContext(Dispatchers.IO) {
        files.flatMap { parseFile(it.file) }
    }

    suspend fun getAllNotes(): List<Note> = withContext(Dispatchers.IO) {
        val files = getSortedNoteFiles()
        parseNotes(files)
    }

    suspend fun getNotesForDate(noteFile: NoteFile?): List<Note> = withContext(Dispatchers.IO) {
        val file = noteFile?.file ?: return@withContext emptyList()
        parseFile(file)
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
                val journals = mutableListOf<Note>()
                for (item in node.children) {
                    if (item.type.name == "LIST_ITEM") {
                        val itemText = item.getTextInNode(content).toString()

                        val lines = itemText.lines()
                        if (lines.isEmpty()) continue

                        val firstLine = lines[0].trim()
                        val match = Regex("^[-*+]\\s+(\\d{2}:\\d{2})\\s*(.*)").find(firstLine)

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

                            journals.add(Note(dateStr, time, contentBuilder.toString().trim()))
                        }
                    }
                }

                journals.sortByDescending { it.datetime }
                notes.addAll(journals)
            }
        }
        return notes
    }

    suspend fun addNote(note: Note, noteFile: NoteFile?) = withContext(Dispatchers.IO) {
        val dir = getRootDirectory() ?: return@withContext
        var file = noteFile?.file

        if (file == null) {
            file = dir.createFile("text/markdown", "${note.date}.md")
            if (file != null) {
                createFileContent(file, note.date)
            }
        }

        if (file == null) return@withContext

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

        val noteEntry = note.toEntry()

        if (!foundJournal) {
            val toAppend = "\n\n## Journal\n$noteEntry"
            appendText(file, toAppend)
        } else {
            val prefix = if (currentContent.endsWith("\n")) "" else "\n"
            appendText(file, prefix + noteEntry)
        }
    }

    suspend fun updateNote(noteFile: NoteFile, index: Int, newContent: String) =
        withContext(Dispatchers.IO) {
            val file = noteFile.file

            val notes = parseFile(file).toMutableList()
            if (index in notes.indices) {
                val oldNote = notes[index]
                notes[index] = oldNote.copy(content = newContent)
                rewriteFile(file, notes)
            }
        }

    suspend fun deleteNote(noteFile: NoteFile, index: Int) = withContext(Dispatchers.IO) {
        val file = noteFile.file

        val notes = parseFile(file).toMutableList()
        if (index in notes.indices) {
            notes.removeAt(index)
            rewriteFile(file, notes)
        }
    }

    private fun createFileContent(file: DocumentFile, date: String) {
        val now = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + "+09:00"

        val template = prefs.noteTemplate
        val text = template.replace("{{date}}", now)
        writeText(file, text)
    }

    private fun rewriteFile(file: DocumentFile, notes: List<Note>) {
        val content = readText(file) ?: return
        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(content)

        var journalStartOffset = 0
        var journalEndOffset = -1
        var foundJournal = false

        for (node in parsedTree.children) {
            if (node.type.name == "ATX_2") {
                val text = node.getTextInNode(content).toString().trim()
                if (text.endsWith("Journal")) {
                    foundJournal = true
                    journalStartOffset = node.startOffset
                    continue
                } else if (foundJournal) {
                    journalEndOffset = node.startOffset
                    break
                }
            }
        }

        val note = buildString {
            append(content.take(journalStartOffset))
            append("## Journal\n")
            append(notesToEntry(notes))

            if (-1 < journalEndOffset && journalEndOffset < content.length) {
                val rest = content.substring(journalEndOffset)
                if (rest.isNotBlank() && !rest.startsWith("\n")) append("\n")
                append(rest)
            }
        }

        writeText(file, note)
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
            context.contentResolver.openOutputStream(file.uri, "wt")?.use {
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