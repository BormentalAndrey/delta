package com.launcher.multiapp

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.jbselfcompany.tyr.TyrApplication
import com.jbselfcompany.tyr.service.YggmailService
import com.jbselfcompany.tyr.utils.AutoconfigServer
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

        // Если регистрация уже пройдена — сразу в чат
        if (appPrefs.getBoolean("registration_completed", false)) {
            launchDeltaChat()
            finish()
            return
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = NeonCyan,
                    secondary = NeonPurple,
                    background = DarkBackground,
                    surface = SurfaceGray,
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MainScreen(
                        isLoading = isLoading.value,
                        loadingMessage = loadingMessage.value,
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Анонимный аккаунт",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
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
                    label = { Text("Ваше имя", color = NeonCyan) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonCyan
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = { Text("Пароль", color = NeonPurple) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = NeonPurple.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonPurple,
                        errorBorderColor = Color.Red
                    ),
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it, color = Color.Red) } }
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
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Создать аккаунт", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
    onLaunchEmail: () -> Unit,
    onSetupAnonymous: (String, String) -> Unit,
    onOpenDialog: () -> Unit,
    onLaunchTyr: () -> Unit
) {
    // Анимация пульсации для логотипа
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Анимация градиента
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )

    val animatedGradient = Brush.verticalGradient(
        colors = listOf(
            DeepPurple,
            Color(0xFF0D0020).copy(alpha = 0.9f),
            DarkBackground
        ),
        startY = 0f + gradientShift * 200f,
        endY = 1000f + gradientShift * 200f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedGradient)
    ) {
        // Декоративные элементы
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset((-100).dp, (-50).dp)
                .clip(CircleShape)
                .background(NeonPurple.copy(alpha = 0.05f))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(250.dp, 600.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.05f))
        )

        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Анимированная иконка загрузки
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.2f))
                        .border(3.dp, NeonPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏳", fontSize = 32.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = loadingMessage,
                    color = NeonCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.width(200.dp),
                    color = NeonPurple,
                    trackColor = NeonPurple.copy(alpha = 0.2f)
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
            // Логотип приложения intro1.png
            Image(
                painter = painterResource(id = R.drawable.intro1),
                contentDescription = "Как дела?",
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Как дела?",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Защищённое общение без границ",
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Кнопка 1: Email
            Button(
                onClick = onLaunchEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .border(1.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📧", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Войти по email",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Text(
                        "Стандартная регистрация",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка 2: Анонимный
            Button(
                onClick = onOpenDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, NeonPurple.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.2f))
                        .border(1.dp, NeonPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Анонимный аккаунт",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple
                    )
                    Text(
                        "Автоматическая настройка",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Скрытая кнопка настроек
            Row(
                modifier = Modifier
                    .clickable { onLaunchTyr() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "⚙️",
                    fontSize = 16.sp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )
            }
        }
    }
}
