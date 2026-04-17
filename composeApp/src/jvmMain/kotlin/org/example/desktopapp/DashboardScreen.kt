package org.example.desktopapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun DashboardScreen(username: String,
                    onLogout: () -> Unit,
                    onViewRecords: () -> Unit,
                    onViewUsers: () -> Unit,
                    onViewTestRecords: () -> Unit,
                    onViewTestResults: () -> Unit) {

    val scope = rememberCoroutineScope()
    val authService = remember { AuthService() }

    // Upload state
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var uploadStatus by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Welcome, $username", style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(40.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Upload File", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        // Show selected file name, or placeholder
        Text(
            text = selectedFile?.name ?: "No file selected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        // Browse button — opens native OS file picker
        OutlinedButton(onClick = {
            // AWT FileDialog = native OS file chooser on Desktop
            val dialog = FileDialog(Frame(), "Select File", FileDialog.LOAD)
            dialog.isVisible = true
            if (dialog.file != null) {
                selectedFile = File(dialog.directory + dialog.file)
                uploadStatus = "" // clear old status
            }
        }) {
            Text("Browse File")
        }

        Spacer(Modifier.height(12.dp))

        // Upload button — only enabled if a file is selected and not already uploading
        Button(
            onClick = {
                val file = selectedFile ?: return@Button
                scope.launch {
                    isUploading = true
                    uploadStatus = ""
                    try {
                        val response = authService.uploadFile(file)
                        uploadStatus = "✓ $response"
                    } catch (e: Exception) {
                        uploadStatus = "✗ Upload failed: ${e.message}"
                    } finally {
                        isUploading = false
                    }
                }
            },
            enabled = selectedFile != null && !isUploading
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Uploading...")
            } else {
                Text("Upload")
            }
        }

        // Status message
        if (uploadStatus.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = uploadStatus,
                color = if (uploadStatus.startsWith("✓"))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(40.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        OutlinedButton(onClick = onLogout) {
            Text("Logout")
        }

        Button(onClick = onViewRecords) {
            Text("View Records")
        }
        Button(onClick = onViewUsers){
            Text("View Users")
        }
        Button(onClick = { onViewTestRecords() }) {
            Text("Test Records")
        }
        Button(
            onClick = onViewTestResults,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Test Results")
        }
    }
}