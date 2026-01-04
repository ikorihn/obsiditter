package com.ikorihn.obsiditter.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
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
import java.util.Calendar

class HomeViewModel(private val repository: NoteRepository) : ViewModel() {
    var notes by mutableStateOf<List<Note>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isEndReached by mutableStateOf(false)
        private set

    var wakeTime by mutableStateOf<String?>(null)
        private set
    var sleepTime by mutableStateOf<String?>(null)
        private set
    var mealLog by mutableStateOf<NoteRepository.MealLog?>(null)
        private set
    var exerciseLog by mutableStateOf<NoteRepository.ExerciseLog?>(null)
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
                loadTodayMetadata()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun loadTodayMetadata() {
        val todayFile = repository.getTodayNoteFile() ?: return
        wakeTime = repository.getFrontmatterValue(todayFile, "wake_time")
        sleepTime = repository.getFrontmatterValue(todayFile, "sleep_time")

        val mealLogs = repository.getMealLogs()
        mealLog = mealLogs.find { it.date == todayFile.name.removeSuffix(".md") }

        val exerciseLogs = repository.getExerciseLogs()
        exerciseLog = exerciseLogs.find { it.date == todayFile.name.removeSuffix(".md") }
    }

    fun updateTime(key: String, time: String) {
        viewModelScope.launch {
            val todayFile = repository.getTodayNoteFile() ?: return@launch
            repository.updateFrontmatterValue(todayFile, key, time)
            if (key == "wake_time") wakeTime = time
            if (key == "sleep_time") sleepTime = time
        }
    }

    fun updateMeal(key: String, items: List<String>) {
        updateMeals(mapOf(key to items))
    }

    fun updateMeals(updates: Map<String, List<String>>) {
        viewModelScope.launch {
            val todayFile = repository.getTodayNoteFile() ?: return@launch
            repository.updateFrontmatterValues(todayFile, updates)
            loadTodayMetadata()
        }
    }

    fun updateExercise(items: List<String>) {
        viewModelScope.launch {
            val todayFile = repository.getTodayNoteFile() ?: return@launch
            repository.updateFrontmatterValue(todayFile, "exercise", items)
            loadTodayMetadata()
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
        val file = allFiles.find { it.name == "${note.date}.md" }
        val dailyNotes = repository.getNotesForDate(file)
        return dailyNotes.indexOfFirst { it.datetime == note.datetime && it.content == note.content }
    }

    fun addNote(content: String, tags: String) {
        if (!repository.isStorageConfigured()) return
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val time = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

            val fullContent = if (tags.isNotBlank()) {
                val tagString = tags.split(" ")
                    .joinToString(" ") { if (it.startsWith("#")) it else "#$it" }
                "$content\n$tagString"
            } else {
                content
            }

            val noteFile = allFiles.find { it.name == "${date}.md" }
            repository.addNote(Note(date, time, fullContent), noteFile)
            loadNotes()
        }
    }

    fun deleteNote(note: Note) {
        if (!repository.isStorageConfigured()) return

        viewModelScope.launch {
            val file = allFiles.find { it.name == "${note.date}.md" }
            if (file == null) return@launch

            val index = findNoteIndex(note)
            if (index != -1) {
                repository.deleteNote(file, index)
                loadNotes()
            }
        }
    }

    fun updateNote(note: Note, newContent: String) {
        if (!repository.isStorageConfigured()) return
        viewModelScope.launch {
            val file = allFiles.find { it.name == "${note.date}.md" }
            if (file == null) return@launch

            val index = findNoteIndex(note)
            if (index != -1) {
                repository.updateNote(file, index, newContent)
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
    onSettings: () -> Unit,
    onMenu: () -> Unit,
    onNavigateToMealTracker: () -> Unit,
    onNavigateToExerciseTracker: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(context))
    var showDeleteDialog by remember { mutableStateOf<Note?>(null) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    val scope = rememberCoroutineScope()

    var inputContent by remember { mutableStateOf("") }
    var inputTags by remember { mutableStateOf("") }

    var showTimePicker by remember { mutableStateOf<String?>(null) }
    var showMealDialog by remember { mutableStateOf(false) }
    var showExerciseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadNotes()
    }

    if (showMealDialog) {
        val mealLog = viewModel.mealLog
        var morningText by remember { mutableStateOf(mealLog?.morning?.joinToString("\n") ?: "") }
        var lunchText by remember { mutableStateOf(mealLog?.lunch?.joinToString("\n") ?: "") }
        var dinnerText by remember { mutableStateOf(mealLog?.dinner?.joinToString("\n") ?: "") }
        var snacksText by remember { mutableStateOf(mealLog?.snacks?.joinToString("\n") ?: "") }

        AlertDialog(
            onDismissRequest = { showMealDialog = false },
            title = { Text("Today's Meals") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = morningText,
                        onValueChange = { morningText = it },
                        label = { Text("Morning") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lunchText,
                        onValueChange = { lunchText = it },
                        label = { Text("Lunch") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dinnerText,
                        onValueChange = { dinnerText = it },
                        label = { Text("Dinner") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = snacksText,
                        onValueChange = { snacksText = it },
                        label = { Text("Snacks") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updates = mapOf(
                        "morning" to morningText.lines().filter { it.isNotBlank() },
                        "lunch" to lunchText.lines().filter { it.isNotBlank() },
                        "dinner" to dinnerText.lines().filter { it.isNotBlank() },
                        "snacks" to snacksText.lines().filter { it.isNotBlank() }
                    )
                    viewModel.updateMeals(updates)
                    showMealDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMealDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExerciseDialog) {
        val exerciseLog = viewModel.exerciseLog
        var exerciseText by remember {
            mutableStateOf(
                exerciseLog?.exercise?.joinToString("\n") ?: ""
            )
        }

        AlertDialog(
            onDismissRequest = { showExerciseDialog = false },
            title = { Text("Today's Exercise") },
            text = {
                OutlinedTextField(
                    value = exerciseText,
                    onValueChange = { exerciseText = it },
                    label = { Text("Exercise (one per line)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateExercise(exerciseText.lines().filter { it.isNotBlank() })
                    showExerciseDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExerciseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTimePicker != null) {
        val initialTime =
            if (showTimePicker == "wake_time") viewModel.wakeTime else viewModel.sleepTime
        val calendar = Calendar.getInstance()
        if (!initialTime.isNullOrBlank()) {
            try {
                val parts = initialTime.split(":")
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
            } catch (e: Exception) {
                // Ignore invalid format
            }
        }

        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    val time =
                        String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    viewModel.updateTime(showTimePicker!!, time)
                    showTimePicker = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = null }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = { Text("Obsiditter") },
                    navigationIcon = {
                        IconButton(onClick = onMenu) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    actions = {
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showTimePicker = "wake_time" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.WbSunny, contentDescription = "Wake Time")
                        Spacer(Modifier.width(4.dp))
                        Text("${viewModel.wakeTime ?: "--:--"}")
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = "sleep_time" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Nightlight, contentDescription = "Sleep Time")
                        Spacer(Modifier.width(4.dp))
                        Text("${viewModel.sleepTime ?: "--:--"}")
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showMealDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Restaurant, contentDescription = "Meal")
                        Spacer(Modifier.width(4.dp))
                        val mealCount = viewModel.mealLog?.let {
                            it.morning.size + it.lunch.size + it.dinner.size + it.snacks.size
                        } ?: 0
                        Text("$mealCount items")
                    }
                    OutlinedButton(
                        onClick = { showExerciseDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = "Exercise")
                        Spacer(Modifier.width(4.dp))
                        val exerciseCount = viewModel.exerciseLog?.exercise?.size ?: 0
                        Text("$exerciseCount items")
                    }
                }
            }
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
                            onEdit = { noteToEdit = note },
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

    if (noteToEdit != null) {
        EditNoteDialog(
            note = noteToEdit!!,
            onDismiss = { noteToEdit = null },
            onConfirm = { newContent ->
                viewModel.updateNote(noteToEdit!!, newContent)
                noteToEdit = null
            }
        )
    }
}

@Composable
fun EditNoteDialog(
    note: Note,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var content by remember { mutableStateOf(note.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Memo") },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                label = { Text("Content") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(content) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
                    text = note.datetime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
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
