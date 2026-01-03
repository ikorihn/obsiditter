package com.ikorihn.obsiditter.ui.mealtracker

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikorihn.obsiditter.data.NoteRepository
import com.ikorihn.obsiditter.data.NoteRepository.MealLog
import kotlinx.coroutines.launch

class MealTrackerViewModel(private val repository: NoteRepository) : ViewModel() {
    var logs by mutableStateOf<List<MealLog>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    fun loadLogs() {
        if (!repository.isStorageConfigured()) return
        viewModelScope.launch {
            isLoading = true
            try {
                logs = repository.getMealLogs()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun updateMeal(log: MealLog, key: String, value: List<String>) {
        viewModelScope.launch {
            repository.updateFrontmatterValue(log.file, key, value)
            loadLogs()
        }
    }
}

class MealTrackerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MealTrackerViewModel(NoteRepository(context)) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTrackerScreen(
    onMenu: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: MealTrackerViewModel = viewModel(factory = MealTrackerViewModelFactory(context))

    LaunchedEffect(Unit) {
        viewModel.loadLogs()
    }

    var editDialogState by remember { mutableStateOf<Triple<MealLog, String, List<String>>?>(null) }
    // Triple: (Log, Key, CurrentValue)

    if (editDialogState != null) {
        val (log, key, initialValue) = editDialogState!!
        var textValue by remember { mutableStateOf(initialValue.joinToString("\n")) }

        AlertDialog(
            onDismissRequest = { editDialogState = null },
            title = { Text("Edit ${key.replaceFirstChar { it.uppercase() }}") },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("$key (one item per line)") },
                    minLines = 3,
                    maxLines = 10
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val list = textValue.lines().filter { it.isNotBlank() }
                    viewModel.updateMeal(log, key, list)
                    editDialogState = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editDialogState = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Meal Tracker") },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records found.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(viewModel.logs) { log ->
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = log.date, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MealButton(
                                    label = "Morning",
                                    value = log.morning,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    editDialogState = Triple(log, "morning", log.morning)
                                }
                                MealButton(
                                    label = "Lunch",
                                    value = log.lunch,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    editDialogState = Triple(log, "lunch", log.lunch)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MealButton(
                                    label = "Dinner",
                                    value = log.dinner,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    editDialogState = Triple(log, "dinner", log.dinner)
                                }
                                MealButton(
                                    label = "Snacks",
                                    value = log.snacks,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    editDialogState = Triple(log, "snacks", log.snacks)
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun MealButton(
    label: String,
    value: List<String>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = if (value.isEmpty()) "--" else value.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }
    }
}
