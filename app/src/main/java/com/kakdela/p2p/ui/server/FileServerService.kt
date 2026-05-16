package com.kakdela.p2p.ui.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import java.io.File
import java.net.NetworkInterface
import java.util.*

class FileServerService : Service() {

    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

        // Предотвращаем засыпание устройства во время работы сервера
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "P2P:ServerWakeLock").apply {
            acquire(10 * 60 * 1000L /* 10 минут макс */)
        }

        startServer(File(filePath))
        return START_NOT_STICKY
    }

    private fun startServer(file: File) {
        serviceScope.launch {
            try {
                val ip = getLocalIpAddress() ?: "127.0.0.1"
                val serverUrl = "http://$ip:$PORT/download"

                server = embeddedServer(CIO, port = PORT, host = ip) {
                    routing {
                        get("/download") {
                            // Продакшен-настройка: передаем имя файла в заголовках, чтобы браузер корректно его сохранил
                            call.response.header(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.name).toString()
                            )
                            call.respondFile(file)
                        }
                    }
                }.start(wait = false)

                // Обновляем уведомление и отправляем Broadcast в UI
                updateNotification("Сервер запущен. Скачайте файл по QR-коду.")
                sendBroadcast(Intent(ACTION_SERVER_STARTED).putExtra(EXTRA_SERVER_URL, serverUrl))

            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES = Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "P2P Sharing", NotificationManager.IMPORTANCE_LOW)
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
