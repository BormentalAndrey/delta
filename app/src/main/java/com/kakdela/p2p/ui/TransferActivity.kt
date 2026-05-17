package com.kakdela.p2p.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.kakdela.p2p.ui.server.FileServerService
import java.io.File
import java.io.FileOutputStream

class TransferActivity : ComponentActivity() {

    private val viewModel: TransferViewModel by viewModels()

    private val serverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FileServerService.ACTION_SERVER_STARTED) {
                val url = intent.getStringExtra(FileServerService.EXTRA_SERVER_URL) ?: ""
                viewModel.onServerStarted(url)
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { startSharing(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter(FileServerService.ACTION_SERVER_STARTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serverReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serverReceiver, filter)
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.uiState.collectAsState()
                    TransferScreen(
                        state = state,
                        onSelectFileClick = { filePickerLauncher.launch("*/*") },
                        onStopSharingClick = { stopSharing() }
                    )
                }
            }
        }
    }

    private fun startSharing(uri: Uri) {
        viewModel.setStartingStatus()

        val cachedFile = copyUriToCache(uri)
        if (cachedFile == null) {
            Toast.makeText(this, "Ошибка при подготовке файла", Toast.LENGTH_SHORT).show()
            viewModel.resetState()
            return
        }

        val intent = Intent(this, FileServerService::class.java).apply {
            putExtra(FileServerService.EXTRA_FILE_PATH, cachedFile.absolutePath)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка запуска сервера: ${e.message}", Toast.LENGTH_SHORT).show()
            viewModel.resetState()
        }
    }

    private fun stopSharing() {
        stopService(Intent(this, FileServerService::class.java))
        viewModel.resetState()
    }

    private fun copyUriToCache(uri: Uri): File? {
        return try {
            val returnCursor = contentResolver.query(uri, null, null, null, null) ?: return null
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            val name = returnCursor.getString(nameIndex)
            returnCursor.close()

            val file = File(cacheDir, name)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(serverReceiver)
        } catch (e: Exception) {
            // receiver already unregistered
        }
    }
}

@Composable
fun TransferScreen(
    state: TransferState,
    onSelectFileClick: () -> Unit,
    onStopSharingClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state) {
            is TransferState.Idle -> {
                Text(
                    text = "Поделиться файлом с кем угодно",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Получателю не нужно приложение. Он сможет скачать файл через браузер, отсканировав QR-код.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onSelectFileClick) {
                    Text("Выбрать файл")
                }
            }

            is TransferState.StartingServer -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Запуск локального сервера...")
            }

            is TransferState.ServerReady -> {
                Text(
                    text = "Всё готово!",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Подключите получателя к вашей Wi-Fi сети и попросите отсканировать этот код:"
                )
                Spacer(modifier = Modifier.height(24.dp))

                Image(
                    bitmap = state.qrCode.asImageBitmap(),
                    contentDescription = "QR Code Link",
                    modifier = Modifier.size(250.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.url,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onStopSharingClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Остановить раздачу")
                }
            }
        }
    }
}
