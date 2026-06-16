package com.texasdex.wtmdappthatdoesntsuck.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.texasdex.wtmdappthatdoesntsuck.WTMDApplication
import com.texasdex.wtmdappthatdoesntsuck.BuildConfig
import com.texasdex.wtmdappthatdoesntsuck.ui.viewmodel.SettingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    highlightSection: String? = null
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as WTMDApplication).repository
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(repository)
    )

    val apiUrl by viewModel.apiUrl.collectAsState()
    val preferredService by viewModel.preferredService.collectAsState()
    val buildDate = BuildConfig.BUILD_TIME
    val scrollState = rememberScrollState()
    var pendingJsonToSave by remember { mutableStateOf<String?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            pendingJsonToSave?.let { json ->
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                pendingJsonToSave = null
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (content != null) {
                viewModel.importBackup(content) {
                    // Feedback could be added here
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings / About") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text("About", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text("Build Date: $buildDate")
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://texasdex.com"))
                    context.startActivity(intent)
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Visit TexasDex.com")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Gordon is a Linux Sysadmin who has never written an Android app before, but hated the new WTMD app so much that he was inspired to make a replacement that actually makes song discovery easier and doesn't demand your location data every time it starts up. If it has bugs well, no big surprise, this whole thing was built in a few days with Gemini, but at least it actually does what he wants.\n" +
                        "Standard disclaimer™: This app is completely unofficial and not affiliated with WTMD whatsoever, which is how you know it's good.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            
            val isMusicHighlighted = highlightSection == "music"
            Column(
                modifier = if (isMusicHighlighted) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
                        .padding(8.dp)
                } else {
                    Modifier
                }
            ) {
                Text("Preferred Music Service", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text("Select where album art clicks should take you:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))

                SettingsViewModel.MUSIC_SERVICES.forEach { service ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = preferredService == service,
                            onClick = { viewModel.setPreferredService(service) }
                        )
                        Text(
                            text = if (service == "None") "None (Art does nothing)" else service,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            
            Text("App Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = apiUrl,
                onValueChange = { viewModel.setApiUrl(it) },
                label = { Text("WTMD API URL") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Change this only if the WTMD API endpoint changes.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(24.dp))
            Text("Data Backup", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        viewModel.exportBackup { json ->
                            val file = File(context.cacheDir, "wtmd_liked_songs_backup.json")
                            file.writeText(json)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Backup"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("Share", fontSize = 12.sp)
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = {
                        viewModel.exportBackup { json ->
                            pendingJsonToSave = json
                            createDocumentLauncher.launch("wtmd_backup_${System.currentTimeMillis()}.json")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("Save to File", fontSize = 12.sp)
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = { importLauncher.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("Restore", fontSize = 12.sp)
                }
            }
        }
    }
}
