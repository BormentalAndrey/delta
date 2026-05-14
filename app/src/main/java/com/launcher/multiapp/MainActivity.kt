package com.launcher.multiapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.jbselfcompany.tyr.TyrApplication
import com.jbselfcompany.tyr.service.YggmailService
import com.jbselfcompany.tyr.utils.AutoconfigServer
import com.kakdela.p2p.ui.navigation.NavGraph
import com.kakdela.p2p.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.thoughtcrime.securesms.BaseConversationListFragment
import org.thoughtcrime.securesms.ConversationActivity
import java.net.InetSocketAddress
import java.net.Socket

// Цветовая схема проекта
private val NeonCyan = Color(0xFF00FFFF)
private val NeonPurple = Color(0xFFB042FF)
private val DarkBackground = Color(0xFF0A0A0A)
private val SurfaceGray = Color(0xFF1E1E1E)
private val DeepPurple = Color(0xFF1A0033)

class MainActivity : FragmentActivity(), BaseConversationListFragment.ConversationSelectedListener {

    private val configRepository by lazy { TyrApplication.instance.configRepository }
    private val autoconfigServer by lazy { AutoconfigServer(this) }
    private val appPrefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    // Состояние Onboarding
    private var isLoading = mutableStateOf(false)
    private var loadingMessage = mutableStateOf("")
    private var showAnonymousDialog = mutableStateOf(false)
    private var isRegistered = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Проверяем, был ли завершен вход ранее
        isRegistered.value = appPrefs.getBoolean("registration_completed", false)

        setContent {
            AppTheme {
                val navController = rememberNavController()

                if (isRegistered.value) {
                    // Основной интерфейс (Чат, Дела, Досуг)
                    NavGraph(
                        navController = navController,
                        startDestination = Routes.CHATS
                    )
                } else {
                    // Экран регистрации/входа
                    MainOnboardingScreen(
                        isLoading = isLoading.value,
                        loadingMessage = loadingMessage.value,
                        onLaunchEmail = {
                            // Логика стандартного входа через Delta Chat
                            markRegistrationCompleted()
                            isRegistered.value = true
                        },
                        onOpenDialog = { showAnonymousDialog.value = true },
                        onLaunchTyr = { launchTyrSettings() }
                    )

                    if (showAnonymousDialog.value) {
                        AnonymousRegistrationDialog(
                            onDismiss = { showAnonymousDialog.value = false },
                            onConfirm = { name, pass ->
                                showAnonymousDialog.value = false
                                setupP2PAccount(name, pass)
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Логика автоматической настройки P2P-аккаунта
     */
    private fun setupP2PAccount(name: String, password: String) {
        isLoading.value = true
        loadingMessage.value = "Инициализация P2P-узла..."

        lifecycleScope.launch {
            try {
                // 1. Сохраняем базовый конфиг в Tyr
                withContext(Dispatchers.IO) {
                    try {
                        configRepository.javaClass.methods
                            .find { it.name == "saveDisplayName" }
                            ?.invoke(configRepository, name)
                    } catch (_: Exception) {}
                    configRepository.savePassword(password)
                    configRepository.setOnboardingCompleted(true)
                }

                // 2. Запускаем YggmailService, если он еще не в работе
                if (!YggmailService.isRunning) {
                    YggmailService.start(this@MainActivity)
                }

                // 3. Ждем готовности IMAP/SMTP порта на 127.0.0.1
                waitForServiceReady()

                // 4. Генерируем магическую ссылку для Delta Chat
                val email = configRepository.getMailAddress()
                if (email.isNullOrEmpty()) throw Exception("Сеть не назначила адрес")

                loadingMessage.value = "Синхронизация с мессенджером..."
                val dcloginUrl = autoconfigServer.generateDcloginUrl(email, password)
                
                // 5. Отправляем Intent в Delta Chat (внутри этого же APK)
                openDeltaChatDeepLink(dcloginUrl)

                markRegistrationCompleted()
                isRegistered.value = true
                
            } catch (e: Exception) {
                isLoading.value = false
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading.value = false
            }
        }
    }

    private suspend fun waitForServiceReady() = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val timeout = 120_000L // 2 минуты на холодный запуск P2P

        while (System.currentTimeMillis() - startTime < timeout) {
            if (YggmailService.isRunning && isPortOpen("127.0.0.1", 1143)) {
                withContext(Dispatchers.Main) { loadingMessage.value = "Сеть найдена!" }
                delay(2000)
                return@withContext
            }
            withContext(Dispatchers.Main) { 
                loadingMessage.value = "Поиск P2P пиров..." 
            }
            delay(3000)
        }
        throw Exception("Таймаут подключения к P2P")
    }

    private fun isPortOpen(host: String, port: Int): Boolean {
        return try {
            Socket().use { it.connect(InetSocketAddress(host, port), 2000) }
            true
        } catch (_: Exception) { false }
    }

    private fun openDeltaChatDeepLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Компонент чата не готов", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markRegistrationCompleted() {
        appPrefs.edit().putBoolean("registration_completed", true).apply()
    }

    private fun launchTyrSettings() {
        val intent = Intent(this, com.jbselfcompany.tyr.ui.MainActivity::class.java)
        startActivity(intent)
    }

    // Обработка клика по чату в нативном фрагменте
    override fun onCreateConversation(chatId: Int) {
        val intent = Intent(this, ConversationActivity::class.java).apply {
            putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId)
        }
        startActivity(intent)
    }

    override fun onSwitchToArchive() {}
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NeonCyan,
            secondary = NeonPurple,
            background = DarkBackground,
            surface = SurfaceGray
        ),
        content = content
    )
}
