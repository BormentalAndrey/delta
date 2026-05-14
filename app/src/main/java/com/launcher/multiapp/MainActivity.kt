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
import androidx.compose.ui.window.Dialog
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
        
        // Проверяем статус регистрации
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
                    // Экран приветствия и регистрации
                    MainScreen(
                        isLoading = isLoading.value,
                        loadingMessage = loadingMessage.value,
                        onLaunchEmail = {
                            markRegistrationCompleted()
                            isRegistered.value = true
                        },
                        onOpenDialog = { showAnonymousDialog.value = true },
                        onLaunchTyr = { launchTyrSettings() }
                    )

                    if (showAnonymousDialog.value) {
                        AnonymousRegistrationDialog(
                            onDismiss = { showAnonymousDialog.value = false },
                            onConfirm = { name: String, pass: String ->
                                showAnonymousDialog.value = false
                                setupP2PAccount(name, pass)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun setupP2PAccount(name: String, password: String) {
        isLoading.value = true
        loadingMessage.value = "Инициализация P2P-узла..."

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    try {
                        configRepository.javaClass.methods
                            .find { it.name == "saveDisplayName" }
                            ?.invoke(configRepository, name)
                    } catch (_: Exception) {}
                    configRepository.savePassword(password)
                    configRepository.setOnboardingCompleted(true)
                }

                if (!YggmailService.isRunning) {
                    YggmailService.start(this@MainActivity)
                }

                waitForServiceReady()

                val email = configRepository.getMailAddress()
                if (email.isNullOrEmpty()) throw Exception("Сеть не назначила адрес")

                loadingMessage.value = "Синхронизация..."
                val dcloginUrl = autoconfigServer.generateDcloginUrl(email, password)
                
                openDeltaChatDeepLink(dcloginUrl)

                markRegistrationCompleted()
                isRegistered.value = true
                
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading.value = false
            }
        }
    }

    private suspend fun waitForServiceReady() = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val timeout = 120_000L

        while (System.currentTimeMillis() - startTime < timeout) {
            if (YggmailService.isRunning && isPortOpen("127.0.0.1", 1143)) {
                withContext(Dispatchers.Main) { loadingMessage.value = "Сеть найдена!" }
                delay(1500)
                return@withContext
            }
            withContext(Dispatchers.Main) { loadingMessage.value = "Поиск P2P пиров..." }
            delay(3000)
        }
        throw Exception("Таймаут P2P")
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
            Toast.makeText(this, "DeltaChat не найден", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markRegistrationCompleted() {
        appPrefs.edit().putBoolean("registration_completed", true).apply()
    }

    private fun launchTyrSettings() {
        val intent = Intent(this, com.jbselfcompany.tyr.ui.MainActivity::class.java)
        startActivity(intent)
    }

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

@Composable
fun MainScreen(
    isLoading: Boolean,
    loadingMessage: String,
    onLaunchEmail: () -> Unit,
    onOpenDialog: () -> Unit,
    onLaunchTyr: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(DeepPurple, DarkBackground))
    )) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.intro1),
                contentDescription = null,
                modifier = Modifier.size(180.dp).scale(scale)
            )
            Spacer(Modifier.weight(1f))

            if (isLoading) {
                CircularProgressIndicator(color = NeonCyan)
                Spacer(Modifier.height(16.dp))
                Text(loadingMessage, color = NeonCyan, textAlign = TextAlign.Center)
            } else {
                Button(
                    onClick = onLaunchEmail,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, NeonCyan)
                ) {
                    Text("Войти по Email", color = NeonCyan)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onOpenDialog,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, NeonPurple)
                ) {
                    Text("Анонимный аккаунт", color = NeonPurple)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Настройки сети",
                modifier = Modifier.clickable { onLaunchTyr() }.padding(8.dp),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun AnonymousRegistrationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGray),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Создать узел", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { if (name.isNotBlank() && password.length >= 6) onConfirm(name, password) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Готово")
                }
            }
        }
    }
}
