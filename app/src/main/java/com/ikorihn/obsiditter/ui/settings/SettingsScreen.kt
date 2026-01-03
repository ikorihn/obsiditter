package com.ikorihn.obsiditter.ui.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.ikorihn.obsiditter.data.NoteRepository
import com.ikorihn.obsiditter.data.Prefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var currentUri by remember { mutableStateOf(prefs.storageUri) }
    var noteTemplate by remember { mutableStateOf(prefs.noteTemplate) }

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

    var categoryUris by remember {
        mutableStateOf(
            NoteRepository.Category.values().associateWith { prefs.getCategoryUri(it) }
        )
    }

    // Creating launchers manually because they must be created during composition (not in loop/map)
    val moviesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleCategoryUri(context, prefs, NoteRepository.Category.Movies, uri) {
                categoryUris = categoryUris + it
            }
        }
    val readingLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleCategoryUri(context, prefs, NoteRepository.Category.Reading, uri) {
                categoryUris = categoryUris + it
            }
        }
    val mangaLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleCategoryUri(context, prefs, NoteRepository.Category.Manga, uri) {
                categoryUris = categoryUris + it
            }
        }
    val liveLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleCategoryUri(context, prefs, NoteRepository.Category.Live, uri) {
                categoryUris = categoryUris + it
            }
        }
    val videoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleCategoryUri(context, prefs, NoteRepository.Category.Video, uri) {
                categoryUris = categoryUris + it
            }
        }
    val radioLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleCategoryUri(context, prefs, NoteRepository.Category.Radio, uri) {
                categoryUris = categoryUris + it
            }
        }
    val youtubeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleCategoryUri(context, prefs, NoteRepository.Category.YouTube, uri) {
                categoryUris = categoryUris + it
            }
        }

    val categoryLaunchers = mapOf(
        NoteRepository.Category.Movies to moviesLauncher,
        NoteRepository.Category.Reading to readingLauncher,
        NoteRepository.Category.Manga to mangaLauncher,
        NoteRepository.Category.Live to liveLauncher,
        NoteRepository.Category.Video to videoLauncher,
        NoteRepository.Category.Radio to radioLauncher,
        NoteRepository.Category.YouTube to youtubeLauncher
    )

    LaunchedEffect(noteTemplate) {
        prefs.noteTemplate = noteTemplate
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
            Text(
                text = "Core Storage",
                style = MaterialTheme.typography.titleLarge
            )
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

            Text(
                text = "Category Storage",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Optional: If not set, subfolders in root will be used.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            NoteRepository.Category.values().forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = categoryUris[category]?.path
                                ?: "Default (root/${category.folderName})",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = { categoryLaunchers[category]?.launch(null) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Select")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Note Template",
                style = MaterialTheme.typography.titleLarge
            )
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

private fun handleCategoryUri(
    context: android.content.Context,
    prefs: Prefs,
    category: NoteRepository.Category,
    uri: Uri?,
    onUpdate: (Pair<NoteRepository.Category, Uri>) -> Unit
) {
    if (uri != null) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        prefs.setCategoryUri(category, uri)
        onUpdate(category to uri)
    }
}