package com.ikorihn.obsiditter.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikorihn.obsiditter.data.NoteRepository
import com.ikorihn.obsiditter.model.Note

class HomeViewModel(private val repository: NoteRepository) : ViewModel() {
    var notes by mutableStateOf<List<Note>>(emptyList())
        private set

    fun loadNotes() {
        notes = repository.getAllNotes()
    }

    fun deleteNote(note: Note) {
        // Find index. This is a bit tricky since we flattened the list.
        // We need to know the index IN THE FILE.
        // For now, let's assume we can find it by content/time matching or reload.
        // But the repository delete expects (date, index).
        // I need to map the flat list back to file index?
        // Or change repository to find match.
        // Let's reload and find index.
        val dailyNotes = repository.getNotesForDate(note.date)
        val index = dailyNotes.indexOfFirst { it.time == note.time && it.content == note.content }
        if (index != -1) {
            repository.deleteNote(note.date, index)
            loadNotes()
        }
    }
}

// Simple factory helper
class HomeViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(NoteRepository(context)) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddNote: () -> Unit,
    onEditNote: (String, Int) -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(context))
    var showDeleteDialog by remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadNotes()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Obsiditter") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNote) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(viewModel.notes) { _, note ->
                    NoteItem(
                        note = note,
                        onEdit = { 
                             // Find index for this note to pass to edit
                             // Re-instantiate repo is cheap enough or ask VM
                             // ideally VM should provide this.
                             // For this prototype, I'll calculate index here roughly
                             // But finding index in the list of "All Notes" is different from "Index in File".
                             // We need "Index in File".
                             // Let's delegate finding index to VM or Repository logic if possible.
                             // Or better: Note object should carry its index? No, index changes.
                             
                             // Quick hack: Search again in VM.
                             // But wait, the callback expects (date, index).
                             // I'll calculate it on the fly.
                             val repo = NoteRepository(context)
                             val dailyNotes = repo.getNotesForDate(note.date)
                             val index = dailyNotes.indexOfFirst { it.time == note.time && it.content == note.content }
                             if (index != -1) {
                                 onEditNote(note.date, index)
                             }
                        },
                        onDelete = { showDeleteDialog = note }
                    )
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
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = note.content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
