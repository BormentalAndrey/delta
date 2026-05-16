package com.kakdela.p2p.ui.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Base64
import androidx.core.app.NotificationCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.NetworkInterface
import java.util.*

class FileServerService : Service() {

    private var server: ApplicationEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverUrl: String = ""

    companion object {
        private const val CHANNEL_ID = "p2p_server_channel"
        private const val NOTIFICATION_ID = 101
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val ACTION_SERVER_STARTED = "com.kakdela.p2p.SERVER_STARTED"
        const val EXTRA_SERVER_URL = "extra_server_url"
        const val PORT = 8080
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val filePath = intent?.getStringExtra(EXTRA_FILE_PATH)
        if (filePath == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Подготовка к передаче..."))

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "P2P:ServerWakeLock").apply {
            acquire(10 * 60 * 1000L)
        }

        startServer(File(filePath))
        return START_NOT_STICKY
    }

    private fun startServer(file: File) {
        serviceScope.launch {
            try {
                val ip = getLocalIpAddress() ?: "127.0.0.1"
                serverUrl = "http://$ip:$PORT"
                val downloadUrl = "$serverUrl/download"
                val fileName = file.name
                val fileSize = formatFileSize(file.length())

                server = embeddedServer(CIO, port = PORT, host = "0.0.0.0") {
                    routing {
                        // Главная страница с QR-кодом и информацией о файле
                        get("/") {
                            val qrBase64 = generateQrCodeBase64(downloadUrl)
                            call.respondText(
                                buildHtmlPage(fileName, fileSize, downloadUrl, qrBase64),
                                ContentType.Text.Html
                            )
                        }

                        // Скачивание файла
                        get("/download") {
                            this.call.response.header(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Attachment.withParameter(
                                    ContentDisposition.Parameters.FileName,
                                    fileName
                                ).toString()
                            )
                            this.call.respondFile(file)
                        }
                    }
                }.start(wait = false)

                updateNotification("Сервер запущен. Скачайте файл по QR-коду.")
                sendBroadcast(
                    Intent(ACTION_SERVER_STARTED)
                        .putExtra(EXTRA_SERVER_URL, serverUrl)
                        .setPackage(packageName)
                )

            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    private fun generateQrCodeBase64(url: String): String {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, 300, 300)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            bitmap.recycle()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun buildHtmlPage(
        fileName: String,
        fileSize: String,
        downloadUrl: String,
        qrBase64: String
    ): String {
        return """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Скачать файл</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        padding: 20px;
                    }
                    .container {
                        background: white;
                        border-radius: 24px;
                        padding: 40px 30px;
                        max-width: 400px;
                        width: 100%;
                        text-align: center;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                    }
                    .icon {
                        font-size: 48px;
                        margin-bottom: 16px;
                    }
                    h1 {
                        font-size: 24px;
                        color: #333;
                        margin-bottom: 8px;
                    }
                    .file-name {
                        font-size: 16px;
                        color: #666;
                        margin-bottom: 4px;
                        word-break: break-all;
                    }
                    .file-size {
                        font-size: 14px;
                        color: #999;
                        margin-bottom: 24px;
                    }
                    .qr-container {
                        background: #f5f5f5;
                        border-radius: 16px;
                        padding: 20px;
                        margin-bottom: 24px;
                        display: inline-block;
                    }
                    .qr-container img {
                        display: block;
                        max-width: 250px;
                        height: auto;
                    }
                    .qr-hint {
                        font-size: 14px;
                        color: #999;
                        margin-bottom: 20px;
                    }
                    .download-btn {
                        display: inline-block;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        text-decoration: none;
                        padding: 14px 32px;
                        border-radius: 50px;
                        font-size: 16px;
                        font-weight: 600;
                        transition: transform 0.2s, box-shadow 0.2s;
                        box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
                    }
                    .download-btn:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
                    }
                    .url-text {
                        margin-top: 16px;
                        font-size: 12px;
                        color: #bbb;
                        word-break: break-all;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">📁</div>
                    <h1>Файл доступен для скачивания</h1>
                    <p class="file-name">$fileName</p>
                    <p class="file-size">$fileSize</p>
                    <div class="qr-container">
                        <img src="data:image/png;base64,$qrBase64" alt="QR-код для скачивания">
                    </div>
                    <p class="qr-hint">Отсканируйте QR-код или нажмите кнопку ниже</p>
                    <a href="$downloadUrl" class="download-btn">⬇ Скачать файл</a>
                    <p class="url-text">$downloadUrl</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes Б"
            bytes < 1024 * 1024 -> "${bytes / 1024} КБ"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} МБ"
            else -> "${bytes / (1024 * 1024 * 1024)} ГБ"
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress ?: continue
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4 && (sAddr.startsWith("192.168.") || sAddr.startsWith("10.") || sAddr.startsWith("172."))) {
                            return sAddr
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Локальный обмен файлами")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "P2P Sharing",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        server?.stop(1000, 2000)
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
