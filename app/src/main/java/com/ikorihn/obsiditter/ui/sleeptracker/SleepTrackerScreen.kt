package com.ikorihn.obsiditter.ui.sleeptracker

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikorihn.obsiditter.data.NoteFile
import com.ikorihn.obsiditter.data.NoteRepository
import com.ikorihn.obsiditter.data.NoteRepository.DailyLog
import kotlinx.coroutines.launch
import java.util.Calendar

class SleepTrackerViewModel(private val repository: NoteRepository) : ViewModel() {
    var logs by mutableStateOf<List<DailyLog>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    fun loadLogs() {
        if (!repository.isStorageConfigured()) return
        viewModelScope.launch {
            isLoading = true
            try {
                logs = repository.getDailyLogs()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun updateTime(log: DailyLog, key: String, time: String) {
        viewModelScope.launch {
            repository.updateFrontmatterValue(log.file, key, time)
            loadLogs() // Reload to reflect changes
        }
    }
}

class SleepTrackerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SleepTrackerViewModel(NoteRepository(context)) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTrackerScreen(
    onMenu: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SleepTrackerViewModel = viewModel(factory = SleepTrackerViewModelFactory(context))

    LaunchedEffect(Unit) {
        viewModel.loadLogs()
    }

    // State for TimePicker Dialog
    var showTimePicker by remember { mutableStateOf<Triple<DailyLog, String, String?>?>(null) } 
    // Triple: (Log, Key ("wake_time" or "sleep_time"), CurrentValue)

    if (showTimePicker != null) {
        val (log, key, initialTime) = showTimePicker!!
        val calendar = Calendar.getInstance()
        if (!initialTime.isNullOrBlank()) {
            try {
                val parts = initialTime.split(":")
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
            } catch (e: Exception) {
                // Ignore
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
                    val time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    viewModel.updateTime(log, key, time)
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
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Sleep Tracker") },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
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
                        ListItem(
                            headlineContent = { Text(log.date) },
                            supportingContent = {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showTimePicker = Triple(log, "wake_time", log.wakeTime) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.WbSunny, contentDescription = "Wake")
                                        Spacer(Modifier.width(4.dp))
                                        Text(log.wakeTime ?: "--:--")
                                    }
                                    OutlinedButton(
                                        onClick = { showTimePicker = Triple(log, "sleep_time", log.sleepTime) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Nightlight, contentDescription = "Sleep")
                                        Spacer(Modifier.width(4.dp))
                                        Text(log.sleepTime ?: "--:--")
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
