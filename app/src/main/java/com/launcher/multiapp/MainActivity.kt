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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jbselfcompany.tyr.TyrApplication
import com.jbselfcompany.tyr.service.YggmailService
import com.jbselfcompany.tyr.utils.AutoconfigServer
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private val configRepository by lazy { TyrApplication.instance.configRepository }
    private val autoconfigServer by lazy { AutoconfigServer(this) }
    private val appPrefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    private var isLoading = mutableStateOf(false)
    private var loadingMessage = mutableStateOf("")
    private var showAnonymousDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Если регистрация уже пройдена — сразу в чат
        if (appPrefs.getBoolean("registration_completed", false)) {
            launchDeltaChat()
            finish()
            return
        }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF7C3AED),
                    secondary = Color(0xFF06B6D4),
                    background = Color(0xFFF8F7FF),
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
                        showDialog = showAnonymousDialog.value,
                        onDismissDialog = { showAnonymousDialog.value = false },
                        onLaunchEmail = { markCompletedAndLaunch { launchDeltaChat() } },
                        onSetupAnonymous = { name, password ->
                            showAnonymousDialog.value = false
                            setupAnonymousAccount(name, password)
                        },
                        onOpenDialog = { showAnonymousDialog.value = true },
                        onLaunchTyr = { launchTyr() }
                    )
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

    private fun markRegistrationCompleted() {
        appPrefs.edit().putBoolean("registration_completed", true).apply()
    }

    private fun markCompletedAndLaunch(action: () -> Unit) {
        markRegistrationCompleted()
        action()
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

    // ========== Запуск Tyr (скрытая кнопка) ==========
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

    // ========== Анонимный аккаунт ==========
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
                    Toast.makeText(this@MainActivity, "Аккаунт создан! Добро пожаловать в Как дела?", Toast.LENGTH_SHORT).show()
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

// ========== Диалог анонимной регистрации ==========
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🛡️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Анонимный аккаунт",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Придумайте имя и пароль.\nВсё остальное настроится автоматически.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ваше имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = { Text("Пароль") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        when {
                            name.isBlank() -> passwordError = "Введите имя"
                            password.length < 6 -> passwordError = "Минимум 6 символов"
                            else -> onConfirm(name.trim(), password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Создать аккаунт", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = Color.Gray)
                }
            }
        }
    }
}

// ========== Главный экран ==========
@Composable
fun MainScreen(
    isLoading: Boolean,
    loadingMessage: String,
    showDialog: Boolean,
    onDismissDialog: () -> Unit,
    onLaunchEmail: () -> Unit,
    onSetupAnonymous: (String, String) -> Unit,
    onOpenDialog: () -> Unit,
    onLaunchTyr: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7C3AED),
                        Color(0xFF8B5CF6),
                        Color(0xFF06B6D4)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💬", fontSize = 56.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Как дела?",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Защищённое общение без границ",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(56.dp))

            Button(
                onClick = onLaunchEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "📧  Войти по email",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "🛡️  Анонимный аккаунт",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            TextButton(onClick = onLaunchTyr) {
                Text(
                    "⚙️",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}
