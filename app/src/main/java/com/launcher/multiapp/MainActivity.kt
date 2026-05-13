package com.launcher.multiapp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalIndication
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.rememberNavController
import com.jbselfcompany.tyr.TyrApplication
import com.jbselfcompany.tyr.service.YggmailService
import com.jbselfcompany.tyr.utils.AutoconfigServer
import com.kakdela.p2p.ui.navigation.NavGraph
import com.kakdela.p2p.ui.navigation.Routes
import kotlinx.coroutines.*

private val NeonCyan = Color(0xFF00FFFF)
private val NeonGreen = Color(0xFF00FF9D)
private val NeonPurple = Color(0xFFB042FF)
private val DarkBackground = Color(0xFF0A0A0A)
private val SurfaceGray = Color(0xFF1E1E1E)
private val DeepPurple = Color(0xFF1A0033)

class MainActivity : ComponentActivity() {

    private val configRepository by lazy { TyrApplication.instance.configRepository }
    private val autoconfigServer by lazy { AutoconfigServer(this) }
    private val appPrefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    private var isLoading = mutableStateOf(false)
    private var loadingMessage = mutableStateOf("")
    private var showAnonymousDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (appPrefs.getBoolean("registration_completed", false)) {
            setContent {
                CompositionLocalProvider(LocalIndication provides ripple()) {
                    MaterialTheme(
                        colorScheme = darkColorScheme(
                            primary = NeonCyan,
                            secondary = NeonPurple,
                            background = DarkBackground,
                            surface = SurfaceGray,
                        )
                    ) {
                        Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
                            val navController = rememberNavController()
                            NavGraph(
                                navController = navController,
                                startDestination = Routes.CHATS
                            )
                        }
                    }
                }
            }
            return
        }

        setContent {
            CompositionLocalProvider(LocalIndication provides ripple()) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = NeonCyan,
                        secondary = NeonPurple,
                        background = DarkBackground,
                        surface = SurfaceGray,
                    )
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
                        MainScreen(
                            isLoading = isLoading.value,
                            loadingMessage = loadingMessage.value,
                            onLaunchEmail = {
                                markRegistrationCompleted()
                                recreate()
                            },
                            onSetupAnonymous = { name, password ->
                                showAnonymousDialog.value = false
                                setupAnonymousAccount(name, password)
                            },
                            onOpenDialog = { showAnonymousDialog.value = true },
                            onLaunchTyr = { launchTyr() }
                        )
                    }
                }
            }

            if (showAnonymousDialog.value) {
                AnonymousRegistrationDialog(
                    onDismiss = { showAnonymousDialog.value = false },
                    onConfirm = { name, password ->
                        showAnonymousDialog.value = false
                        setupAnonymousAccount(name, password)
                    }
                )
            }
        }
    }

    // ... остальные методы без изменений ...
    private fun markRegistrationCompleted() {
        appPrefs.edit().putBoolean("registration_completed", true).apply()
    }

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

    private fun setupAnonymousAccount(name: String, password: String) {
        configRepository.savePassword(password)
        configRepository.setOnboardingCompleted(true)

        isLoading.value = true
        loadingMessage.value = "Создание защищённого аккаунта..."

        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (!YggmailService.isRunning) {
                    YggmailService.start(this@MainActivity)
                }

                withContext(Dispatchers.IO) {
                    waitForServiceReady()
                }

                val email = configRepository.getMailAddress()

                if (email.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Не удалось создать аккаунт.", Toast.LENGTH_LONG).show()
                        isLoading.value = false
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    loadingMessage.value = "Настройка DeltaChat..."
                }
                val dcloginUrl = autoconfigServer.generateDcloginUrl(email, password)

                withContext(Dispatchers.Main) {
                    markRegistrationCompleted()
                    openDeltaChatWithDclogin(dcloginUrl)
                    isLoading.value = false
                    Toast.makeText(this@MainActivity, "Аккаунт создан!", Toast.LENGTH_SHORT).show()
                    recreate()
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

    private suspend fun waitForServiceReady(timeoutMs: Long = 120000L) {
        val startTime = System.currentTimeMillis()
        var imapWasReady = false

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (YggmailService.isRunning) {
                val email = configRepository.getMailAddress()
                val imapReady = isImapReady()

                if (!email.isNullOrEmpty() && imapReady) {
                    if (!imapWasReady) {
                        imapWasReady = true
                        withContext(Dispatchers.Main) {
                            loadingMessage.value = "Подключение к сети..."
                        }
                        delay(5000)
                    }
                    if (imapWasReady) {
                        withContext(Dispatchers.Main) {
                            loadingMessage.value = "Всё готово!"
                        }
                        delay(1500)
                        return
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadingMessage.value = "Запуск сервера..."
                    }
                }
            }
            delay(2000)
        }
        throw IllegalStateException("Таймаут ожидания")
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

    private fun openDeltaChatWithDclogin(dcloginUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(dcloginUrl)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "DeltaChat не найден", Toast.LENGTH_LONG).show()
                launchDeltaChat()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            launchDeltaChat()
        }
    }
}

// ... остальные Composable функции без изменений ...
