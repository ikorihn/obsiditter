package com.ikorihn.obsiditter.ui.categorytracker

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikorihn.obsiditter.data.NoteRepository
import com.ikorihn.obsiditter.data.NoteRepository.CategoryRecord
import com.ikorihn.obsiditter.data.Prefs
import com.ikorihn.obsiditter.model.Category
import com.ikorihn.obsiditter.model.CategoryField
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

    fun addRecord(category: Category, date: String, fields: Map<String, Any>, body: String) {
        viewModelScope.launch {
            repository.addCategoryRecord(category, date, fields, body)
            loadRecords(category)
        }
    }

    fun updateRecord(record: CategoryRecord, date: String, fields: Map<String, Any>, body: String) {
        viewModelScope.launch {
            repository.updateCategoryRecord(record, date, fields, body)
            loadRecords(record.category)
        }
    }

    fun deleteRecord(record: CategoryRecord) {
        viewModelScope.launch {
            repository.deleteCategoryRecord(record)
            loadRecords(record.category)
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
    val prefs = remember { Prefs(context) }
    val viewModel: CategoryTrackerViewModel =
        viewModel(factory = CategoryTrackerViewModelFactory(context))
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<CategoryRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<CategoryRecord?>(null) }

    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    if (showAddDialog && selectedCategory != null) {
        AddRecordDialog(
            category = selectedCategory!!,
            onDismiss = { showAddDialog = false },
            onConfirm = { date, fields, body ->
                viewModel.addRecord(selectedCategory!!, date, fields, body)
                showAddDialog = false
            }
        )
    }

    val currentRecordToEdit = recordToEdit
    if (currentRecordToEdit != null) {
        AddRecordDialog(
            category = currentRecordToEdit.category,
            initialDate = currentRecordToEdit.date,
            initialFields = currentRecordToEdit.fields,
            initialBody = currentRecordToEdit.body,
            isEdit = true,
            onDismiss = { recordToEdit = null },
            onConfirm = { date, fields, body ->
                viewModel.updateRecord(currentRecordToEdit, date, fields, body)
                recordToEdit = null
            }
        )
    }

    val currentRecordToDelete = recordToDelete
    if (currentRecordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Record") },
            text = { Text("Are you sure you want to delete this record?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(currentRecordToDelete)
                    recordToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    categories = prefs.categories,
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
                            RecordItem(
                                record = record,
                                onEdit = { recordToEdit = record },
                                onDelete = { recordToDelete = record }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGrid(categories: List<Category>, onCategorySelected: (Category) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(categories) { category ->
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
fun RecordItem(
    record: CategoryRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                val title = record.fields["title"]?.toString() ?: "Untitled"
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = record.date, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        record.category.fields.filter { it.key != "title" }.forEach { field ->
            val value = record.fields[field.key]
            if (value != null && value.toString().isNotBlank()) {
                Text(
                    text = "${field.displayName}: $value",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (record.body.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(text = record.body, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
        }
    }
}

@Composable
fun AddRecordDialog(
    category: Category,
    initialDate: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
    initialFields: Map<String, Any> = emptyMap(),
    initialBody: String = "",
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Map<String, Any>, String) -> Unit
) {
    var date by remember { mutableStateOf(initialDate) }
    var fields by remember { mutableStateOf(initialFields) }
    var body by remember { mutableStateOf(initialBody) }

    AlertDialog(
        onDismissRequest = { /* Do nothing to prevent dismissal */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.9f),
        title = {
            Text(
                if (isEdit) "Edit ${category.displayName}" else "Add ${category.displayName}"
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                category.fields.forEach { field ->
                    val value = fields[field.key]?.let {
                        if (it is List<*>) it.joinToString("\n") else it.toString()
                    } ?: ""

                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            val updatedFields = fields.toMutableMap()
                            if (field.type == CategoryField.FieldType.List) {
                                updatedFields[field.key] =
                                    newValue.lines().filter { it.isNotBlank() }
                            } else {
                                updatedFields[field.key] = newValue
                            }
                            fields = updatedFields
                        },
                        label = { Text(field.displayName + if (field.type == CategoryField.FieldType.List) " (one per line)" else "") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = if (field.type == CategoryField.FieldType.List) 3 else 1
                    )
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Memo") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 20
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (date.isNotBlank()) {
                        onConfirm(date, fields, body)
                    }
                },
                enabled = date.isNotBlank()
            ) {
                Text(if (isEdit) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
