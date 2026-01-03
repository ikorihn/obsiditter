package com.ikorihn.obsiditter.ui.categorytracker

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.ikorihn.obsiditter.data.NoteRepository.Category
import com.ikorihn.obsiditter.data.NoteRepository.CategoryRecord
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CategoryTrackerViewModel(private val repository: NoteRepository) : ViewModel() {
    var records by mutableStateOf<List<CategoryRecord>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    fun loadRecords(category: Category) {
        if (!repository.isStorageConfigured()) return
        viewModelScope.launch {
            isLoading = true
            try {
                records = repository.getCategoryRecords(category)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun addRecord(category: Category, title: String, date: String, content: String) {
        viewModelScope.launch {
            repository.addCategoryRecord(category, title, date, content)
            loadRecords(category)
        }
    }
}

class CategoryTrackerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CategoryTrackerViewModel(NoteRepository(context)) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTrackerScreen(
    onMenu: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: CategoryTrackerViewModel =
        viewModel(factory = CategoryTrackerViewModelFactory(context))
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    if (showAddDialog && selectedCategory != null) {
        AddRecordDialog(
            category = selectedCategory!!,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, date, content ->
                viewModel.addRecord(selectedCategory!!, title, date, content)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(selectedCategory?.displayName ?: "Collections") },
                navigationIcon = {
                    if (selectedCategory != null) {
                        IconButton(onClick = { selectedCategory = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = onMenu) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedCategory != null) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Record")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (selectedCategory == null) {
                CategoryGrid(
                    onCategorySelected = {
                        selectedCategory = it
                        viewModel.loadRecords(it)
                    }
                )
            } else {
                if (viewModel.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (viewModel.records.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No records yet.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(viewModel.records) { record ->
                            RecordItem(record)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGrid(onCategorySelected: (Category) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(Category.values()) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clickable { onCategorySelected(category) },
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = category.displayName, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun RecordItem(record: CategoryRecord) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = record.title, style = MaterialTheme.typography.titleMedium)
        Text(text = record.date, style = MaterialTheme.typography.labelMedium)
        if (record.content.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(text = record.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
        }
    }
}

@Composable
fun AddRecordDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember {
        mutableStateOf(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
    }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${category.displayName}") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Memo") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && date.isNotBlank()) {
                        onConfirm(title, date, content)
                    }
                },
                enabled = title.isNotBlank() && date.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
