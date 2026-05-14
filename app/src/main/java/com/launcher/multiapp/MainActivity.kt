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
import androidx.compose.ui.platform.LocalContext
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
import com.kakdela.p2p.data.IdentityRepository // Убедитесь, что импорт верный
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

private val NeonCyan = Color(0xFF00FFFF)
private val NeonPurple = Color(0xFFB042FF)
private val DarkBackground = Color(0xFF0A0A0A)
private val SurfaceGray = Color(0xFF1E1E1E)
private val DeepPurple = Color(0xFF1A0033)

class MainActivity : FragmentActivity(), BaseConversationListFragment.ConversationSelectedListener {

    private val configRepository by lazy { TyrApplication.instance.configRepository }
    private val autoconfigServer by lazy { AutoconfigServer(this) }
    private val appPrefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }
    
    // Вам нужно инициализировать ваш IdentityRepository
    private val identityRepository by lazy { 
        // Здесь должен быть ваш реальный инициализатор репозитория
        com.kakdela.p2p.data.IdentityRepository(this) 
    }

    private var isLoading = mutableStateOf(false)
    private var loadingMessage = mutableStateOf("")
    private var showAnonymousDialog = mutableStateOf(false)
    private var isRegistered = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isRegistered.value = appPrefs.getBoolean("registration_completed", false)

        setContent {
            AppTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    if (isRegistered.value) {
                        // Передаем логику отрисовки DeltaChat прямо в NavGraph
                        NavGraph(
                            navController = navController,
                            identityRepository = identityRepository,
                            startDestination = Routes.CHATS,
                            chatLayer = {
                                DeltaChatAndroidView()
                            }
                        )
                    } else {
                        MainScreen(
                            isLoading = isLoading.value,
                            loadingMessage = loadingMessage.value,
                            onLaunchEmail = {
                                markRegistrationCompleted()
                                isRegistered.value = true
                            },
                            onSetupAnonymous = { name, password ->
                                showAnonymousDialog.value = false
                                setupAnonymousAccount(name, password)
                            },
                            onOpenDialog = { showAnonymousDialog.value = true },
                            onLaunchTyr = { launchTyr() }
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

    @Composable
    private fun DeltaChatAndroidView() {
        val context = LocalContext.current
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    id = View.generateViewId()
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    val fragmentManager = supportFragmentManager
                    val fragment = ConversationListFragment().apply {
                        arguments = Bundle().apply { putBoolean("archive", false) }
                    }
                    
                    fragmentManager.beginTransaction()
                        .replace(this.id, fragment, "DELTA_CHAT")
                        .commitNow() // Используем commitNow для немедленной отрисовки
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // --- Остальные методы без изменений (Auth, Tyr, и т.д.) ---

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
            } catch (e2: Exception) {
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

                    withContext(Dispatchers.IO) { waitForServiceReady() }

                    val email = configRepository.getMailAddress()
                    if (email.isNullOrEmpty()) {
                        isLoading.value = false
                        Toast.makeText(this@MainActivity, "Ошибка адреса", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    loadingMessage.value = "Настройка DeltaChat..."
                    val dcloginUrl = autoconfigServer.generateDcloginUrl(email, password)
                    openDeltaChatWithDclogin(dcloginUrl)

                    markRegistrationCompleted()
                    isLoading.value = false
                    isRegistered.value = true
                    Toast.makeText(this@MainActivity, "Аккаунт готов!", Toast.LENGTH_SHORT).show()

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
                        withContext(Dispatchers.Main) { loadingMessage.value = "Подключение..." }
                        delay(5000)
                    }
                    withContext(Dispatchers.Main) { loadingMessage.value = "Всё готово!" }
                    delay(1500)
                    return
                } else {
                    withContext(Dispatchers.Main) { loadingMessage.value = "Запуск сервера..." }
                }
            }
            delay(2000)
        }
        throw IllegalStateException("Таймаут")
    }

    private suspend fun isImapReady(): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", 1143), 2000)
                true
            }
        } catch (_: Exception) { false }
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
