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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

// Цветовая палитра Cyber-Tech
private val NeonCyan = Color(0xFF00FFFF)
private val NeonPurple = Color(0xFFB042FF)
private val DarkBackground = Color(0xFF010101)
private val SurfaceGray = Color(0xFF121212)
private val DeepPurple = Color(0xFF0F0025)

class MainActivity : FragmentActivity(), BaseConversationListFragment.ConversationSelectedListener {

    private val configRepository by lazy { TyrApplication.instance.configRepository }
    private val autoconfigServer by lazy { AutoconfigServer(this) }
    private val appPrefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    // Состояния через делегаты для удобства доступа
    private var isLoading by mutableStateOf(false)
    private var loadingMessage by mutableStateOf("")
    private var showAnonymousDialog by mutableStateOf(false)
    private var isRegistered by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация статуса регистрации
        isRegistered = appPrefs.getBoolean("registration_completed", false)

        setContent {
            AppTheme {
                val navController = rememberNavController()

                if (isRegistered) {
                    NavGraph(
                        navController = navController,
                        startDestination = Routes.CHATS,
                        chatLayer = { DeltaChatLayer() }
                    )
                } else {
                    MainScreen(
                        isLoading = isLoading,
                        loadingMessage = loadingMessage,
                        onLaunchEmail = {
                            markRegistrationCompleted()
                            isRegistered = true
                        },
                        onOpenDialog = { showAnonymousDialog = true },
                        onLaunchTyr = { launchTyrSettings() }
                    )

                    if (showAnonymousDialog) {
                        AnonymousRegistrationDialog(
                            onDismiss = { showAnonymousDialog = false },
                            onConfirm = { name, pass ->
                                showAnonymousDialog = false
                                setupP2PAccount(name, pass)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun DeltaChatLayer() {
        val activity = LocalContext.current as FragmentActivity
        val containerId = remember { View.generateViewId() }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FrameLayout(context).apply {
                    id = containerId
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { _ ->
                // Используем commitNowAllowingStateLoss для предотвращения крэшей при фоновых обновлениях
                val fm = activity.supportFragmentManager
                if (fm.findFragmentById(containerId) == null) {
                    val fragment = ConversationListFragment().apply {
                        arguments = Bundle().apply {
                            putBoolean(ConversationListFragment.ARCHIVE, false)
                        }
                    }
                    fm.beginTransaction()
                        .replace(containerId, fragment)
                        .commitNowAllowingStateLoss()
                }
            }
        )
    }

    private fun setupP2PAccount(name: String, password: String) {
        isLoading = true
        loadingMessage = "Инициализация P2P-узла..."

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Рефлексивный вызов для гибкости API Tyr
                    try {
                        configRepository.javaClass.methods
                            .find { it.name == "saveDisplayName" }
                            ?.invoke(configRepository, name)
                    } catch (_: Exception) { }

                    configRepository.savePassword(password)
                    configRepository.setOnboardingCompleted(true)
                }

                if (!YggmailService.isRunning) {
                    YggmailService.start(this@MainActivity)
                }

                waitForServiceReady()

                val email = configRepository.getMailAddress() ?: throw Exception("Сеть не назначила адрес")

                loadingMessage = "Синхронизация DeltaChat..."
                val dcloginUrl = autoconfigServer.generateDcloginUrl(email, password)
                
                openDeltaChatDeepLink(dcloginUrl)
                markRegistrationCompleted()
                isRegistered = true

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    private suspend fun waitForServiceReady() = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 120_000L) {
            if (YggmailService.isRunning && isPortOpen("127.0.0.1", 1143)) {
                withContext(Dispatchers.Main) { loadingMessage = "Сеть найдена!" }
                delay(1500)
                return@withContext
            }
            withContext(Dispatchers.Main) { loadingMessage = "Поиск P2P-пиров..." }
            delay(3000)
        }
        throw Exception("Таймаут подключения к P2P")
    }

    private fun isPortOpen(host: String, port: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(host, port), 2000) }
        true
    } catch (_: Exception) { false }

    private fun openDeltaChatDeepLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "DeltaChat не установлен", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markRegistrationCompleted() {
        appPrefs.edit().putBoolean("registration_completed", true).apply()
    }

    private fun launchTyrSettings() {
        try {
            val intent = Intent(this, com.jbselfcompany.tyr.ui.MainActivity::class.java)
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Настройки Tyr недоступны", Toast.LENGTH_SHORT).show()
        }
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
            surface = SurfaceGray,
            onSurface = Color.White
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
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(DeepPurple, DarkBackground)))) {
        
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
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
                CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp)
                Spacer(Modifier.height(20.dp))
                Text(loadingMessage, color = NeonCyan, fontSize = 14.sp, textAlign = TextAlign.Center)
            } else {
                OutlinedButton(
                    onClick = onLaunchEmail,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                ) {
                    Text("ВОЙТИ ПО EMAIL", fontWeight = FontWeight.SemiBold)
                }
                
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onOpenDialog,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, NeonPurple)
                ) {
                    Text("АНОНИМНЫЙ АККАУНТ", color = NeonPurple, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.weight(0.8f))
            
            Text(
                "Настройки сети",
                modifier = Modifier.clickable { onLaunchTyr() }.padding(12.dp),
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
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Создать P2P узел", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя пользователя") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                )
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль (мин. 6 симв.)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPurple)
                )
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = { if (name.isNotBlank() && password.length >= 6) onConfirm(name, password) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("СОЗДАТЬ", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
