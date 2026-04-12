package com.tomsphone.feature.carer.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomsphone.core.ui.theme.WandasDimensions
import com.tomsphone.core.ui.theme.wandasColors
import com.tomsphone.feature.carer.CarerSettingsViewModel
import com.tomsphone.feature.carer.components.CarerBreadcrumb
import com.tomsphone.feature.carer.transfer.AppDataTransfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Export / import this app’s contacts and carer settings as a JSON file (e-mail, Drive, USB, etc.).
 * Not included in the system cloud backup; use this when changing phones or before App Reset.
 */
@Composable
fun DataTransferScreen(
    onBack: () -> Unit,
    viewModel: CarerSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                pendingImportUri = uri
                showImportConfirm = true
            }
        },
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.wandasColors.background,
    ) {
        Column(Modifier.fillMaxSize()) {
            CarerBreadcrumb(
                title = "Transfer data",
                parentTitle = "Settings",
                onBack = onBack,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WandasDimensions.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(WandasDimensions.SpacingMedium),
            ) {
                Text(
                    text = "Save a file with contacts and settings from this app, or restore one on this phone. " +
                        "The system does not include this app in normal cloud backup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.wandasColors.onBackground,
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.wandasColors.surface),
                ) {
                    Text(
                        text = "Keep transfer files private. They can include phone numbers, medical notes, and access settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.wandasColors.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.padding(WandasDimensions.SpacingMedium),
                    )
                }
                Button(
                    onClick = {
                        if (busy) return@Button
                        busy = true
                        message = null
                        scope.launch {
                            val result = runCatching {
                                val json = viewModel.buildTransferExportJson()
                                withContext(Dispatchers.IO) {
                                    val dir = File(context.cacheDir, "transfer").apply { mkdirs() }
                                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                                    val file = File(dir, "${AppDataTransfer.FILE_NAME_PREFIX}-$stamp.json")
                                    file.writeText(json)
                                    val authority = "${context.packageName}.transferfileprovider"
                                    val uri = FileProvider.getUriForFile(context, authority, file)
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        clipData = android.content.ClipData.newUri(
                                            context.contentResolver,
                                            "Transfer",
                                            uri,
                                        )
                                    }
                                    context.startActivity(
                                        Intent.createChooser(send, "Share transfer file"),
                                    )
                                }
                            }
                            result.onFailure { message = it.message ?: "Export failed" }
                            busy = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !busy,
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    } else {
                        Text("Export to file…", fontWeight = FontWeight.Medium)
                    }
                }
                Text(
                    "Creates a JSON file you can send by email, save to Files, or copy to another phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onBackground.copy(alpha = 0.65f),
                )
                OutlinedButton(
                    onClick = {
                        if (busy) return@OutlinedButton
                        openDocument.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !busy,
                ) {
                    Text("Import from file…", fontWeight = FontWeight.Medium)
                }
                Text(
                    "Replaces all contacts and carer settings in this app with the file. In-app call history is cleared. " +
                        "Export first if you need a backup of what is on the phone now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.wandasColors.onBackground.copy(alpha = 0.65f),
                )
                message?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showImportConfirm && pendingImportUri != null) {
        val uri = pendingImportUri!!
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                pendingImportUri = null
            },
            title = { Text("Replace data on this phone?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Importing will remove current contacts and settings in this app and replace them from the file. " +
                        "This cannot be undone unless you have another export.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirm = false
                        busy = true
                        message = null
                        scope.launch {
                            val read = runCatching {
                                withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        input.bufferedReader().readText()
                                    } ?: error("Could not read file")
                                }
                            }
                            read.fold(
                                onSuccess = { json ->
                                    val done = viewModel.importTransferFromJson(json)
                                    done.fold(
                                        onSuccess = { message = "Import completed." },
                                        onFailure = { message = it.message ?: "Import failed" },
                                    )
                                },
                                onFailure = { message = it.message ?: "Could not read file" },
                            )
                            busy = false
                            pendingImportUri = null
                        }
                    },
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        pendingImportUri = null
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}
