package com.ikorihn.obsiditter.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikorihn.obsiditter.data.NoteFile
import com.ikorihn.obsiditter.data.NoteRepository
import com.ikorihn.obsiditter.model.Note
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HomeViewModel(private val repository: NoteRepository) : ViewModel() {
    var notes by mutableStateOf<List<Note>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isEndReached by mutableStateOf(false)
        private set

    private var allFiles: List<NoteFile> = emptyList()
    private var currentPage = 0
    private val pageSize = 50

    fun loadNotes() {
        if (!repository.isStorageConfigured()) {
            notes = emptyList()
            return
        }
        viewModelScope.launch {
            isLoading = true
            notes = emptyList()
            currentPage = 0
            isEndReached = false
            try {
                allFiles = repository.getSortedNoteFiles()
                loadMoreNotesInternal()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMoreNotes() {
        if (isLoading || isEndReached) return
        viewModelScope.launch {
            isLoading = true
            loadMoreNotesInternal()
            isLoading = false
        }
    }

    private suspend fun loadMoreNotesInternal() {
        val offset = currentPage * pageSize
        if (offset >= allFiles.size) {
            isEndReached = true
            return
        }

        val limit = minOf(pageSize, allFiles.size - offset)
        val filesToParse = allFiles.subList(offset, offset + limit)

        val newNotes = repository.parseNotes(filesToParse)
        notes = notes + newNotes
        currentPage++

        if (offset + limit >= allFiles.size) {
            isEndReached = true
        }
    }

    suspend fun findNoteIndex(note: Note): Int {
        val dailyNotes = repository.getNotesForDate(note.date)
        return dailyNotes.indexOfFirst { it.time == note.time && it.content == note.content }
    }

    fun addNote(content: String, tags: String) {
        if (!repository.isStorageConfigured()) return
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val time = now.format(DateTimeFormatter.ofPattern("HH:mm"))

            val fullContent = if (tags.isNotBlank()) {
                val tagString = tags.split(" ")
                    .joinToString(" ") { if (it.startsWith("#")) it else "#$it" }
                "$content\n$tagString"
            } else {
                content
            }

            val noteFile = allFiles.find { it.name == "${date}.md"}
            repository.addNote(Note(date, time, fullContent), noteFile)
            loadNotes()
        }
    }

    fun deleteNote(note: Note) {
        if (!repository.isStorageConfigured()) return

        viewModelScope.launch {
            val index = findNoteIndex(note)
            if (index != -1) {
                repository.deleteNote(note.date, index)
                loadNotes()
            }
        }
    }

    private fun minOf(a: Int, b: Int): Int {
        return if (a <= b) a else b
    }
}

class HomeViewModelFactory(private val context: Context) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(NoteRepository(context)) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEditNote: (String, Int) -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(context))
    var showDeleteDialog by remember { mutableStateOf<Note?>(null) }
    val scope = rememberCoroutineScope()

    var inputContent by remember { mutableStateOf("") }
    var inputTags by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadNotes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Obsiditter") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputContent,
                        onValueChange = { inputContent = it },
                        label = { Text("What's happening?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputTags,
                            onValueChange = { inputTags = it },
                            label = { Text("Tags") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputContent.isNotBlank()) {
                                    viewModel.addNote(inputContent, inputTags)
                                    inputContent = ""
                                    inputTags = ""
                                }
                            },
                            enabled = inputContent.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post")
                            Spacer(Modifier.width(4.dp))
                            Text("Post")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (viewModel.notes.isEmpty() && !viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notes found. Check storage settings.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(viewModel.notes) { index, note ->
                        if (index >= viewModel.notes.size - 1 && !viewModel.isEndReached && !viewModel.isLoading) {
                            LaunchedEffect(Unit) {
                                viewModel.loadMoreNotes()
                            }
                        }

                        NoteItem(
                            note = note,
                            onEdit = {
                                scope.launch {
                                    val idx = viewModel.findNoteIndex(note)
                                    if (idx != -1) {
                                        onEditNote(note.date, idx)
                                    }
                                }
                            },
                            onDelete = { showDeleteDialog = note }
                        )
                    }

                    if (viewModel.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog?.let { viewModel.deleteNote(it) }
                    showDeleteDialog = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun NoteItem(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${note.date} ${note.time}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = note.content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}