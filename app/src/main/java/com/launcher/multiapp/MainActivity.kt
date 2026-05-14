package com.launcher.multiapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.ui.viewinterop.AndroidView
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
import org.thoughtcrime.securesms.ConversationListFragment
import java.net.InetSocketAddress
import java.net.Socket

// Цветовая палитра
private val NeonCyan = Color(0xFF00FFFF)
private val NeonGreen = Color(0xFF00FF9D)
private val NeonPurple = Color(0xFFB042FF)
private val DarkBackground = Color(0xFF0A0A0A)
private val SurfaceGray = Color(0xFF1E1E1E)
private val DeepPurple = Color(0xFF1A0033)

class MainActivity :
    FragmentActivity(),
    BaseConversationListFragment.ConversationSelectedListener {

    private val configRepository by lazy {
        TyrApplication.instance.configRepository
    }

    private val autoconfigServer by lazy {
        AutoconfigServer(this)
    }

    private val appPrefs by lazy {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
    }

    private var isLoading = mutableStateOf(false)
    private var loadingMessage = mutableStateOf("")
    private var showAnonymousDialog = mutableStateOf(false)
    private var isRegistered = mutableStateOf(false)

    // Контейнер для DeltaChat
    private var chatContainer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isRegistered.value = appPrefs.getBoolean("registration_completed", false)

        if (isRegistered.value) {
            initChatLayer()
        }

        setContent {
            AppTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    if (isRegistered.value) {
                        NavGraph(
                            navController = navController,
                            startDestination = Routes.CHATS,
                            chatLayer = {
                                AndroidView(
                                    factory = { context ->
                                        if (chatContainer == null) {
                                            initChatLayer()
                                        }
                                        chatContainer!!.apply {
                                            visibility = View.VISIBLE
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    update = { view ->
                                        view.visibility = View.VISIBLE
                                    }
                                )
                            }
                        )
                    } else {
                        // Исправлено: Вызов MainScreen с корректными параметрами
                        MainScreen(
                            isLoading = isLoading.value,
                            loadingMessage = loadingMessage.value,
                            onLaunchEmail = {
                                markRegistrationCompleted()
                                isRegistered.value = true
                                initChatLayer()
                            },
                            onOpenDialog = {
                                showAnonymousDialog.value = true
                            },
                            onLaunchTyr = {
                                launchTyr()
                            }
                        )

                        if (showAnonymousDialog.value) {
                            // Исправлено: явное указание типов параметров (String, String)
                            AnonymousRegistrationDialog(
                                onDismiss = {
                                    showAnonymousDialog.value = false
                                },
                                onConfirm = { name: String, pass: String ->
                                    showAnonymousDialog.value = false
                                    setupAnonymousAccount(name, pass)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun initChatLayer() {
        if (chatContainer != null) return

        chatContainer = FrameLayout(this).apply {
            id = View.generateViewId()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.VISIBLE
        }

        val existingFragment = supportFragmentManager.findFragmentByTag("DELTA_CHAT")

        if (existingFragment == null) {
            val fragment = ConversationListFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("archive", false)
                }
            }

            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(chatContainer!!.id, fragment, "DELTA_CHAT")
                .commitNowAllowingStateLoss()
        }
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

    override fun onSwitchToArchive() {}

    override fun onCreateConversation(chatId: Int) {
        try {
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка открытия чата", Toast.LENGTH_LONG).show()
        }
    }

    private fun markRegistrationCompleted() {
        appPrefs.edit().putBoolean("registration_completed", true).apply()
    }

    private fun launchTyr() {
        try {
            val intent = Intent(this, com.jbselfcompany.tyr.ui.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(this, com.jbselfcompany.tyr.ui.onboarding.OnboardingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Tyr не найден", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupAnonymousAccount(name: String, password: String) {
        try {
            try {
                configRepository.javaClass.methods
                    .find { it.name == "saveDisplayName" }
                    ?.invoke(configRepository, name)
            } catch (_: Exception) {}

            configRepository.savePassword(password)
            configRepository.setOnboardingCompleted(true)

            isLoading.value = true
            loadingMessage.value = "Создание защищённого аккаунта..."

            lifecycleScope.launch {
                try {
                    if (!YggmailService.isRunning) {
                        YggmailService.start(this@MainActivity)
                    }

                    withContext(Dispatchers.IO) {
                        waitForServiceReady()
                    }

                    val email = configRepository.getMailAddress()

                    if (email.isNullOrEmpty()) {
                        isLoading.value = false
                        Toast.makeText(this@MainActivity, "Не удалось создать аккаунт", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    loadingMessage.value = "Настройка DeltaChat..."
                    val dcloginUrl = autoconfigServer.generateDcloginUrl(email, password)

                    try {
                        openDeltaChatWithDclogin(dcloginUrl)
                    } catch (_: Exception) {}

                    markRegistrationCompleted()
                    initChatLayer()
                    isLoading.value = false
                    isRegistered.value = true
                    Toast.makeText(this@MainActivity, "Аккаунт создан!", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    isLoading.value = false
                    Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            isLoading.value = false
            Toast.makeText(this, "Ошибка инициализации", Toast.LENGTH_LONG).show()
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
                    withContext(Dispatchers.Main) {
                        loadingMessage.value = "Всё готово!"
                    }
                    delay(1500)
                    return
                }
                withContext(Dispatchers.Main) {
                    loadingMessage.value = "Запуск сервера..."
                }
            }
            delay(2000)
        }
        throw IllegalStateException("Таймаут ожидания")
    }

    private suspend fun isImapReady(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", 1143), 2000)
                    true
                }
            } catch (_: Exception) {
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
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "DeltaChat не найден", Toast.LENGTH_LONG).show()
        }
    }
}

// --- Компоненты интерфейса ---

@Composable
fun MainScreen(
    isLoading: Boolean,
    loadingMessage: String,
    onLaunchEmail: () -> Unit,
    onOpenDialog: () -> Unit,
    onLaunchTyr: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = NeonCyan)
                Spacer(Modifier.height(16.dp))
                Text(loadingMessage, color = Color.White)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onOpenDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Создать P2P аккаунт", color = Color.White)
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onLaunchTyr) {
                    Text("Открыть Tyr", color = NeonCyan)
                }
            }
        }
    }
}

@Composable
fun AnonymousRegistrationDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, password: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceGray),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🛡️", fontSize = 28.sp)
                Spacer(Modifier.height(16.dp))
                Text("Анонимный аккаунт", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ваше имя", color = NeonCyan) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    label = { Text("Пароль", color = NeonPurple) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = { passwordError?.let { Text(it, color = Color.Red) } },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPurple, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (name.isBlank()) passwordError = "Введите имя"
                        else if (password.length < 6) passwordError = "Минимум 6 символов"
                        else onConfirm(name.trim(), password)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Создать аккаунт", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
