package com.kakdela.p2p.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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

    // Приемник широковещательных сообщений от Сервиса о старте сервера
    private val serverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FileServerService.ACTION_SERVER_STARTED) {
                val url = intent.getStringExtra(FileServerService.EXTRA_SERVER_URL) ?: ""
                viewModel.onServerStarted(url)
            }
        }
    }

    // Лаунчер для выбора любого файла из системы
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { startSharing(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Регистрируем ресивер для получения URL от запущенного сервера
        val filter = IntentFilter(FileServerService.ACTION_SERVER_STARTED)
        registerReceiver(serverReceiver, filter, RECEIVER_NOT_EXPORTED)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
        // Кэшируем файл во внутреннюю директорию, чтобы Ktor имел к нему прямой File-доступ
        val cachedFile = copyUriToCache(uri) ?: return
        
        val intent = Intent(this, FileServerService::class.java).apply {
            putExtra(FileServerService.EXTRA_FILE_PATH, cachedFile.absolutePath)
        }
        startForegroundService(intent)
    }

    private fun stopSharing() {
        stopService(Intent(this, FileServerService::class.java))
        viewModel.resetState()
    }

    private fun copyUriToCache(uri: Uri): File? {
        try {
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
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(serverReceiver)
    }
}

@Composable
fun TransferScreen(
    state: TransferState,
    onSelectFileClick: () -> Unit,
    onStopSharingClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state) {
            is TransferState.Idle -> {
                Text(text = "Поделиться файлом с кем угодно", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Получателю не нужно приложение. Он сможет скачать файл через браузер, отсканировав QR-код.", style = MaterialTheme.typography.bodyMedium)
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
                Text(text = "Всё готово!", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Подключите получателя к вашей Wi-Fi сети (или точке доступа) и попросите отсканировать этот код:")
                Spacer(modifier = Modifier.height(24.dp))
                
                Image(
                    bitmap = state.qrCode.asImageBitmap(),
                    contentDescription = "QR Code Link",
                    modifier = Modifier.size(250.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = state.url, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onStopSharingClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Остановить раздачу")
                }
            }
        }
    }
}
