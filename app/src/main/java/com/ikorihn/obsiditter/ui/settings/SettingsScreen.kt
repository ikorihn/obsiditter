package com.ikorihn.obsiditter.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ikorihn.obsiditter.data.Prefs
import com.ikorihn.obsiditter.model.Category
import com.ikorihn.obsiditter.model.CategoryField
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var currentUri by remember { mutableStateOf(prefs.storageUri) }
    var noteTemplate by remember { mutableStateOf(prefs.noteTemplate) }
    var categories by remember { mutableStateOf(prefs.categories) }

    val rootLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefs.storageUri = uri
            currentUri = uri
        }
    }

    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val categoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val category = categoryToEdit
        if (uri != null && category != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefs.setCategoryUri(category, uri)
            // Trigger UI update
            categories = categories.toList()
        }
    }

    LaunchedEffect(noteTemplate) {
        prefs.noteTemplate = noteTemplate
    }

    LaunchedEffect(categories) {
        prefs.categories = categories
    }

    if (showAddCategoryDialog) {
        CategoryEditDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { newCategory ->
                categories = categories + newCategory
                showAddCategoryDialog = false
            }
        )
    }

    if (categoryToEdit != null) {
        CategoryEditDialog(
            initialCategory = categoryToEdit,
            onDismiss = { categoryToEdit = null },
            onConfirm = { updatedCategory ->
                categories =
                    categories.map { if (it.id == updatedCategory.id) updatedCategory else it }
                categoryToEdit = null
            },
            onSelectFolder = { category ->
                categoryToEdit = category
                categoryLauncher.launch(null)
            }
        )
    }

    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Core Storage", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentUri?.path ?: "Not configured",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { rootLauncher.launch(null) }) {
                Text("Select Root Folder")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Categories", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { showAddCategoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Category")
                }
            }

            categories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleSmall
                        )
                        val uri = prefs.getCategoryUri(category)
                        Text(
                            text = uri?.path ?: "Default (root/${category.folderName})",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = { categoryToEdit = category }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = {
                        categories = categories.filter { it.id != category.id }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Note Template", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteTemplate,
                onValueChange = { noteTemplate = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 10,
                maxLines = 20,
                label = { Text("Template (Use {{date}} for current timestamp)") }
            )

            if (currentUri != null) {
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onNavigateBack) {
                    Text("Done")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CategoryEditDialog(
    initialCategory: Category? = null,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit,
    onSelectFolder: ((Category) -> Unit)? = null
) {
    var displayName by remember { mutableStateOf(initialCategory?.displayName ?: "") }
    var folderName by remember { mutableStateOf(initialCategory?.folderName ?: "") }
    var fields by remember {
        mutableStateOf(
            initialCategory?.fields ?: listOf(
                CategoryField(
                    "title",
                    "Title"
                )
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCategory == null) "Add Category" else "Edit Category") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (onSelectFolder != null && initialCategory != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onSelectFolder(initialCategory) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Storage Folder")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Fields", style = MaterialTheme.typography.titleSmall)
                fields.forEachIndexed { index, field ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = field.displayName, onValueChange = {
                                val newFields = fields.toMutableList()
                                newFields[index] = field.copy(
                                    displayName = it,
                                    key = it.lowercase().replace(" ", "_")
                                )
                                fields = newFields
                            }, label = { Text("Field Label") })
                        }
                        IconButton(onClick = {
                            val newFields = fields.toMutableList()
                            val nextType = when (field.type) {
                                CategoryField.FieldType.ShortText -> CategoryField.FieldType.LongText
                                CategoryField.FieldType.LongText -> CategoryField.FieldType.Select
                                CategoryField.FieldType.Select -> CategoryField.FieldType.List
                                CategoryField.FieldType.List -> CategoryField.FieldType.Date
                                CategoryField.FieldType.Date -> CategoryField.FieldType.ShortText
                            }
                            newFields[index] = field.copy(type = nextType)
                            fields = newFields
                        }) {
                            val icon = when (field.type) {
                                CategoryField.FieldType.List -> Icons.AutoMirrored.Filled.List
                                CategoryField.FieldType.Select -> Icons.Default.Edit
                                else -> Icons.Default.TextFields
                            }
                            Icon(
                                icon,
                                contentDescription = "Toggle Type: ${field.type.name}"
                            )
                        }
                        IconButton(onClick = {
                            val newFields = fields.toMutableList()
                            newFields.removeAt(index)
                            fields = newFields
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Field")
                        }
                    }
                }
                TextButton(onClick = {
                    fields = fields + CategoryField("new_field", "New Field")
                }) {
                    Text("Add Field")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val category = initialCategory?.copy(
                    displayName = displayName,
                    folderName = folderName,
                    fields = fields
                )
                    ?: Category(UUID.randomUUID().toString(), displayName, folderName, fields)
                onConfirm(category)
            }, enabled = displayName.isNotBlank() && folderName.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
