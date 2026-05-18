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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
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
import java.util.concurrent.atomic.AtomicBoolean

// ПРИМЕЧАНИЕ: Убедитесь, что этот импорт соответствует вашему модулю, 
// либо замените R.drawable ниже на прямую ссылку com.jbselfcompany.tyr.R.drawable.intro1
import com.launcher.multiapp.R 

private const val CHAT_CONTAINER_VIEW_ID = 46832

private val NeonCyan = Color(0xFF00FFFF)
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
    private var isServerReady = mutableStateOf(false)

    private val isStartingServer = AtomicBoolean(false)
    private var chatContainer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isRegistered.value =
            appPrefs.getBoolean("registration_completed", false)

        // Проверяем статус IMAP сервера при холодном старте асинхронно
        lifecycleScope.launch(Dispatchers.IO) {
            val ready = isImapReady()
            withContext(Dispatchers.Main) {
                isServerReady.value = ready
            }
        }

        setContent {
            AppTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Инициализация фрагмента чатов строго при готовности сервера и наличии аккаунта
                LaunchedEffect(isRegistered.value, isServerReady.value) {
                    if (isRegistered.value && isServerReady.value) {
                        initChatLayer()
                    }
                }

                LaunchedEffect(currentRoute, isRegistered.value, isServerReady.value) {
                    val isChatsTab = isRegistered.value && currentRoute == Routes.CHATS

                    chatContainer?.visibility =
                        if (isChatsTab && isServerReady.value) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    // Если открыта вкладка чатов, но сокет упал или не запустился — поднимаем службу
                    if (isChatsTab && !isServerReady.value && isStartingServer.compareAndSet(false, true)) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val serverResponding = isImapReady()

                                if (!YggmailService.isRunning || !serverResponding) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Подключение к P2P сети...",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    if (!YggmailService.isRunning) {
                                        YggmailService.start(this@MainActivity)
                                    }

                                    // Защита от вечного зависания: используем надежный дефолтный таймаут в 2 минуты
                                    waitForServiceReady() 
                                }

                                withContext(Dispatchers.Main) {
                                    isServerReady.value = true
                                    if (chatContainer == null) {
                                        initChatLayer()
                                    }
                                    Toast.makeText(
                                        this@MainActivity,
                                        "P2P Сервер подключен!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Ошибка запуска: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } finally {
                                isStartingServer.set(false)
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    if (isRegistered.value) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            NavGraph(
                                navController = navController,
                                startDestination = Routes.CHATS,
                                chatLayer = {
                                    if (isServerReady.value) {
                                        AndroidView(
                                            factory = {
                                                if (chatContainer == null) {
                                                    initChatLayer()
                                                }
                                                // Гарантированно отвязываем View от старого Compose-снапшота во избежание IllegalStateException
                                                (chatContainer?.parent as? ViewGroup)?.removeView(chatContainer)
                                                chatContainer!!
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(DarkBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(color = NeonPurple)
                                                Spacer(Modifier.height(16.dp))
                                                Text(
                                                    "Синхронизация защищенных ключей...",
                                                    color = NeonCyan,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        MainScreen(
                            isLoading = isLoading.value,
                            loadingMessage = loadingMessage.value,
                            onLaunchEmail = {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        withContext(Dispatchers.Main) {
                                            isLoading.value = true
                                            loadingMessage.value = "Подключение..."
                                        }

                                        if (!YggmailService.isRunning) {
                                            YggmailService.start(this@MainActivity)
                                        }

                                        waitForServiceReady()

                                        withContext(Dispatchers.Main) {
                                            markRegistrationCompleted()
                                            isServerReady.value = true
                                            isRegistered.value = true
                                            isLoading.value = false
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isLoading.value = false
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Ошибка: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            onOpenDialog = { showAnonymousDialog.value = true },
                            onLaunchTyr = { launchTyr() },
                            onRestartServer = { restartYggmailService() }
                        )

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
            }
        }
    }

    private fun restartYggmailService() {
        if (!isStartingServer.compareAndSet(false, true)) {
            Toast.makeText(this, "Сервер уже перезапускается", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    isLoading.value = true
                    loadingMessage.value = "Перезапуск сервера..."
                    isServerReady.value = false
                }

                if (YggmailService.isRunning) {
                    YggmailService.stop(this@MainActivity)
                    delay(1000)
                }

                YggmailService.start(this@MainActivity)
                waitForServiceReady()

                withContext(Dispatchers.Main) {
                    isLoading.value = false
                    isServerReady.value = true
                    Toast.makeText(this@MainActivity, "Сервер перезапущен!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading.value = false
                    Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isStartingServer.set(false)
            }
        }
    }

    private fun initChatLayer() {
        if (chatContainer != null) return

        chatContainer = FrameLayout(this).apply {
            id = CHAT_CONTAINER_VIEW_ID
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }

        val fragmentManager = supportFragmentManager
        if (fragmentManager.findFragmentByTag("DELTA_CHAT") == null) {
            val fragment = ConversationListFragment().apply {
                arguments = Bundle().apply { putBoolean("archive", false) }
            }
            fragmentManager.beginTransaction()
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

    override fun onCreateConversation(chatId: Int) {
        val intent = Intent(this, ConversationActivity::class.java).apply {
            putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    override fun onSwitchToArchive() {}

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
            } catch (_: Exception) {
                Toast.makeText(this, "Tyr не найден", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupAnonymousAccount(name: String, password: String) {
        try {
            try {
                configRepository.javaClass.methods.find { it.name == "saveDisplayName" }
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
                        Toast.makeText(this@MainActivity, "Ошибка адреса", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    loadingMessage.value = "Настройка DeltaChat..."
                    val dcloginUrl = autoconfigServer.generateDcloginUrl(email, password)
                    openDeltaChatWithDclogin(dcloginUrl)

                    delay(3000)

                    markRegistrationCompleted()
                    isLoading.value = false
                    isServerReady.value = true
                    isRegistered.value = true

                    Toast.makeText(this@MainActivity, "Аккаунт готов!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    isLoading.value = false
                    Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            isLoading.value = false
            Toast.makeText(this, "Ошибка initialization", Toast.LENGTH_LONG).show()
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
                            loadingMessage.value = "Подключение..."
                        }
                        delay(5000)
                    }
                    withContext(Dispatchers.Main) {
                        loadingMessage.value = "Всё готово!"
                    }
                    delay(1500)
                    return
                } else {
                    withContext(Dispatchers.Main) {
                        loadingMessage.value = "Запуск сервера..."
                    }
                }
            }
            delay(2000)
        }
        throw IllegalStateException("Таймаут")
    }

    private suspend fun isImapReady(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", 1143), 2000)
                    true
                }
            } catch (_: Exception) {
                false
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
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.2f))
                        .border(2.dp, NeonPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 28.sp)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Анонимный аккаунт",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Text(
                    "P2P почта без серверов",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ваше имя", color = NeonCyan) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = { Text("Пароль", color = NeonPurple) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = passwordError?.let {
                        { Text(it, color = Color.Red) }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPurple)
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (name.isBlank() || password.length < 6) {
                            passwordError = "Минимум 6 символов"
                        } else {
                            onConfirm(name.trim(), password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Создать аккаунт", fontWeight = FontWeight.Bold, color = Color.White)
                }

                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    isLoading: Boolean,
    loadingMessage: String,
    onLaunchEmail: () -> Unit,
    onOpenDialog: () -> Unit,
    onLaunchTyr: () -> Unit,
    onRestartServer: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "scale"
    )

    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        DeepPurple,
                        Color(0xFF0D0020).copy(alpha = 0.9f),
                        DarkBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            Image(
                painter = painterResource(id = R.drawable.intro1),
                contentDescription = "Logo",
                modifier = Modifier.size(200.dp).scale(scale)
            )

            Spacer(modifier = Modifier.weight(0.5f))

            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonPurple)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = loadingMessage,
                        color = NeonCyan,
                        modifier = Modifier.alpha(textAlpha),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Button(
                    onClick = onLaunchEmail,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Text("Войти по email", color = NeonCyan, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onOpenDialog,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, NeonPurple.copy(alpha = 0.5f))
                ) {
                    Text("Анонимный аккаунт", color = NeonPurple, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onRestartServer,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔄 Перезапустить сервер", color = Color(0xFF00FFFF), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            IconButton(onClick = onLaunchTyr) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Settings",
                    tint = Color.Gray
                )
            }
        }
    }
}
