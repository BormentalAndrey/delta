package com.launcher.multiapp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jbselfcompany.tyr.TyrApplication
import com.jbselfcompany.tyr.service.YggmailService
import com.jbselfcompany.tyr.utils.AutoconfigServer
import com.jbselfcompany.tyr.utils.PeerManager
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private val configRepository by lazy { TyrApplication.instance.configRepository }
    private val autoconfigServer by lazy { AutoconfigServer(this) }
    
    // Состояния для UI
    private var isLoading = mutableStateOf(false)
    private var loadingMessage = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1976D2),
                    secondary = Color(0xFF388E3C),
                    background = Color(0xFFF5F5F5),
                    surface = Color.White,
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isLoading = isLoading.value,
                        loadingMessage = loadingMessage.value,
                        onLaunchDeltaChat = { launchDeltaChat() },
                        onSetupAnonymous = { setupAnonymousAccount() },
                        onLaunchTyr = { launchTyr() }
                    )
                }
            }
        }
    }

    // ========== Обычный запуск DeltaChat ==========
    private fun launchDeltaChat() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName(packageName, "org.thoughtcrime.securesms.ConversationListActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                startActivity(intent)
            } else {
                tryProfileActivity()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "DeltaChat не найден: ${e.message}", Toast.LENGTH_LONG).show()
            tryProfileActivity()
        }
    }

    private fun tryProfileActivity() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName(packageName, "org.thoughtcrime.securesms.ProfileActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "DeltaChat не найден.", Toast.LENGTH_LONG).show()
        }
    }

    // ========== Запуск Tyr ==========
    private fun launchTyr() {
        try {
            val intent = Intent(this, com.jbselfcompany.tyr.ui.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val intent = Intent(this, com.jbselfcompany.tyr.ui.onboarding.OnboardingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e2: Exception) {
                e2.printStackTrace()
                Toast.makeText(this, "Tyr не найден: ${e2.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ========== Анонимный аккаунт: Поиск пиров + Запуск Yggmail + Автонастройка DeltaChat ==========
    private fun setupAnonymousAccount() {
        if (!configRepository.isOnboardingCompleted()) {
            Toast.makeText(this, "Сначала завершите настройку в Tyr", Toast.LENGTH_LONG).show()
            launchTyr()
            return
        }

        isLoading.value = true
        loadingMessage.value = "Поиск доступных пиров..."

        // Шаг 1: Найти рабочий пир
        PeerManager.findWorkingPeer(object : PeerManager.PeerCheckCallback {
            override fun onStatusUpdate(message: String) {
                runOnUiThread {
                    loadingMessage.value = message
                }
            }

            override fun onPeerFound(peer: String, latencyMs: Long) {
                TyrLogger.d("MainActivity", "Peer found: $peer (${latencyMs}ms)")
                runOnUiThread {
                    loadingMessage.value = "Пир найден (${latencyMs}мс). Запуск сервера..."
                }
                // Сохраняем рабочий пир
                configRepository.savePeer(
                    com.jbselfcompany.tyr.data.PeerInfo(
                        uri = peer,
                        isEnabled = true,
                        tag = com.jbselfcompany.tyr.data.PeerInfo.PeerTag.DEFAULT
                    )
                )
                // Запускаем сервис
                startYggmailAndSetupDeltaChat()
            }

            override fun onAllPeersFailed() {
                runOnUiThread {
                    isLoading.value = false
                    Toast.makeText(
                        this@MainActivity,
                        "Не удалось найти рабочий пир. Проверьте подключение к интернету.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun startYggmailAndSetupDeltaChat() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Шаг 2: Запустить Yggmail сервис
                if (!YggmailService.isRunning) {
                    YggmailService.start(this@MainActivity)
                }

                // Шаг 3: Подождать готовности IMAP порта
                withContext(Dispatchers.IO) {
                    waitForServiceReady()
                }

                // Шаг 4: Получить учётные данные
                val email = configRepository.getMailAddress()
                val password = configRepository.getPassword()

                if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Учётные данные не найдены.", Toast.LENGTH_LONG).show()
                        isLoading.value = false
                        launchTyr()
                    }
                    return@launch
                }

                // Шаг 5: Сгенерировать DCLOGIN и открыть DeltaChat
                withContext(Dispatchers.Main) {
                    loadingMessage.value = "Настройка DeltaChat..."
                }
                val dcloginUrl = autoconfigServer.generateDcloginUrl(email, password)

                withContext(Dispatchers.Main) {
                    openDeltaChatWithDclogin(dcloginUrl)
                    isLoading.value = false
                    Toast.makeText(this@MainActivity, "Анонимный аккаунт готов!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isLoading.value = false
                    Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun runOnUiThread(action: () -> Unit) {
        if (Thread.currentThread() == android.os.Looper.getMainLooper().thread) {
            action()
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post(action)
        }
    }

    /**
     * Ожидание готовности Yggmail IMAP сервера
     */
    private suspend fun waitForServiceReady(timeoutMs: Long = 120000L) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (YggmailService.isRunning) {
                val email = configRepository.getMailAddress()
                if (!email.isNullOrEmpty() && isImapReady()) {
                    // Проверяем, есть ли подключённые пиры
                    if (hasConnectedPeers()) {
                        withContext(Dispatchers.Main) {
                            loadingMessage.value = "Пиры подключены. Настройка DeltaChat..."
                        }
                        delay(3000)
                        return
                    } else {
                        withContext(Dispatchers.Main) {
                            loadingMessage.value = "Ожидание подключения к Yggdrasil сети..."
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadingMessage.value = "Ожидание запуска сервера..."
                    }
                }
            }
            delay(2000)
        }
        throw IllegalStateException("Таймаут ожидания Yggmail (${timeoutMs / 1000}с)")
    }

    private suspend fun isImapReady(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress("127.0.0.1", 1143), 2000)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun hasConnectedPeers(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val instance = YggmailService.instance ?: return@withContext false
                val connections = instance.getPeerConnections()
                connections?.any { it.up } == true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun openDeltaChatWithDclogin(dcloginUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(dcloginUrl)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "DeltaChat не может обработать DCLOGIN", Toast.LENGTH_LONG).show()
                launchDeltaChat()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка открытия DCLOGIN: ${e.message}", Toast.LENGTH_LONG).show()
            launchDeltaChat()
        }
    }
}

@Composable
fun MainScreen(
    isLoading: Boolean,
    loadingMessage: String,
    onLaunchDeltaChat: () -> Unit,
    onSetupAnonymous: () -> Unit,
    onLaunchTyr: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1565C0),
                        Color(0xFF42A5F5),
                        Color(0xFF90CAF9)
                    )
                )
            )
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = Color.White,
                    strokeWidth = 6.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = loadingMessage,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🚀", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Universal Launcher",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Выберите режим запуска",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            ActionButton(
                text = "Анонимный аккаунт",
                subtitle = "Автопоиск пиров, запуск Yggmail и настройка DeltaChat",
                icon = "🛡️",
                color = Color(0xFF6C3483),
                enabled = !isLoading,
                onClick = onSetupAnonymous
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionButton(
                text = "DeltaChat",
                subtitle = "Открыть защищённый мессенджер",
                icon = "💬",
                color = Color(0xFF4CAF50),
                enabled = !isLoading,
                onClick = onLaunchDeltaChat
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionButton(
                text = "Tyr",
                subtitle = "Управление Yggmail сервером и настройками",
                icon = "⚙️",
                color = Color(0xFFFF5722),
                enabled = !isLoading,
                onClick = onLaunchTyr
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Выберите способ подключения",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    subtitle: String,
    icon: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = Color.White,
                disabledContainerColor = color.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}
