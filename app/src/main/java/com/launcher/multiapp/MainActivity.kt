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

    // Главный state приложения
    private var isRegistered = mutableStateOf(false)

    // Постоянный контейнер DeltaChat
    private var chatContainer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isRegistered.value =
            appPrefs.getBoolean("registration_completed", false)

        setContent {
            AppTheme {

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Инициализация DeltaChat слоя
                LaunchedEffect(isRegistered.value) {
                    if (isRegistered.value) {
                        initChatLayer()
                    }
                }

                // Управление видимостью фрагмента
                LaunchedEffect(currentRoute, isRegistered.value) {
                    chatContainer?.visibility =
                        if (
                            isRegistered.value &&
                            currentRoute == Routes.CHATS
                        ) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {

                    if (isRegistered.value) {

                        Box(modifier = Modifier.fillMaxSize()) {

                            // Native DeltaChat Fragment
                            AndroidView(
                                factory = {
                                    if (chatContainer == null) {
                                        initChatLayer()
                                    }
                                    chatContainer!!
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Compose Navigation Overlay
                            NavGraph(
                                navController = navController,
                                startDestination = Routes.CHATS
                            )
                        }

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

                            onOpenDialog = {
                                showAnonymousDialog.value = true
                            },

                            onLaunchTyr = {
                                launchTyr()
                            }
                        )

                        if (showAnonymousDialog.value) {
                            AnonymousRegistrationDialog(
                                onDismiss = {
                                    showAnonymousDialog.value = false
                                },

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

    private fun initChatLayer() {

        if (chatContainer != null) return

        chatContainer = FrameLayout(this).apply {
            id = View.generateViewId()

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            visibility = View.GONE
        }

        val existing =
            supportFragmentManager.findFragmentByTag("DELTA_CHAT")

        if (existing == null) {

            val fragment = ConversationListFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("archive", false)
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(
                    chatContainer!!.id,
                    fragment,
                    "DELTA_CHAT"
                )
                .commitAllowingStateLoss()
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

        val intent = Intent(
            this,
            ConversationActivity::class.java
        ).apply {

            putExtra(
                ConversationActivity.CHAT_ID_EXTRA,
                chatId
            )

            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        startActivity(intent)
    }

    private fun markRegistrationCompleted() {
        appPrefs.edit()
            .putBoolean("registration_completed", true)
            .apply()
    }

    private fun launchTyr() {

        try {

            val intent = Intent(
                this,
                com.jbselfcompany.tyr.ui.MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            startActivity(intent)

        } catch (e: Exception) {

            e.printStackTrace()

            try {

                val intent = Intent(
                    this,
                    com.jbselfcompany.tyr.ui.onboarding.OnboardingActivity::class.java
                ).apply {

                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                startActivity(intent)

            } catch (e2: Exception) {

                e2.printStackTrace()

                Toast.makeText(
                    this,
                    "Tyr не найден",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupAnonymousAccount(
        name: String,
        password: String
    ) {

        try {

            // Если есть поддержка имени
            try {
                configRepository.javaClass
                    .methods
                    .find { it.name == "saveDisplayName" }
                    ?.invoke(configRepository, name)
            } catch (_: Exception) {
            }

            configRepository.savePassword(password)
            configRepository.setOnboardingCompleted(true)

            isLoading.value = true
            loadingMessage.value =
                "Создание защищённого аккаунта..."

            lifecycleScope.launch {

                try {

                    if (!YggmailService.isRunning) {
                        YggmailService.start(this@MainActivity)
                    }

                    withContext(Dispatchers.IO) {
                        waitForServiceReady()
                    }

                    val email =
                        configRepository.getMailAddress()

                    if (email.isNullOrEmpty()) {

                        isLoading.value = false

                        Toast.makeText(
                            this@MainActivity,
                            "Не удалось создать аккаунт",
                            Toast.LENGTH_LONG
                        ).show()

                        return@launch
                    }

                    loadingMessage.value =
                        "Настройка DeltaChat..."

                    val dcloginUrl =
                        autoconfigServer.generateDcloginUrl(
                            email,
                            password
                        )

                    // Можно оставить для совместимости
                    openDeltaChatWithDclogin(dcloginUrl)

                    markRegistrationCompleted()

                    isLoading.value = false

                    Toast.makeText(
                        this@MainActivity,
                        "Аккаунт создан!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Переход без recreate()
                    isRegistered.value = true

                } catch (e: Exception) {

                    e.printStackTrace()

                    isLoading.value = false

                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        } catch (e: Exception) {

            e.printStackTrace()

            isLoading.value = false

            Toast.makeText(
                this,
                "Ошибка инициализации",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private suspend fun waitForServiceReady(
        timeoutMs: Long = 120000L
    ) {

        val startTime = System.currentTimeMillis()
        var imapWasReady = false

        while (
            System.currentTimeMillis() - startTime < timeoutMs
        ) {

            if (YggmailService.isRunning) {

                val email =
                    configRepository.getMailAddress()

                val imapReady = isImapReady()

                if (!email.isNullOrEmpty() && imapReady) {

                    if (!imapWasReady) {

                        imapWasReady = true

                        withContext(Dispatchers.Main) {
                            loadingMessage.value =
                                "Подключение к сети..."
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
                        loadingMessage.value =
                            "Запуск сервера..."
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

                Socket().use { socket ->

                    socket.connect(
                        InetSocketAddress(
                            "127.0.0.1",
                            1143
                        ),
                        2000
                    )

                    true
                }

            } catch (_: Exception) {
                false
            }
        }
    }

    private fun openDeltaChatWithDclogin(
        dcloginUrl: String
    ) {

        try {

            val intent = Intent(Intent.ACTION_VIEW).apply {

                data = Uri.parse(dcloginUrl)

                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            startActivity(intent)

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "DeltaChat не найден",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

// ================= COMPOSABLES =================

@Composable
fun AnonymousRegistrationDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, password: String) -> Unit
) {

    var name by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordError by remember {
        mutableStateOf<String?>(null)
    }

    Dialog(onDismissRequest = onDismiss) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            shape = RoundedCornerShape(24.dp),

            colors = CardDefaults.cardColors(
                containerColor = SurfaceGray
            ),

            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            NeonPurple.copy(alpha = 0.2f)
                        )
                        .border(
                            2.dp,
                            NeonPurple,
                            CircleShape
                        ),

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

                Spacer(Modifier.height(4.dp))

                Text(
                    "Придумайте имя и пароль.\nВсё остальное настроится автоматически.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,

                    onValueChange = {
                        name = it
                    },

                    label = {
                        Text(
                            "Ваше имя",
                            color = NeonCyan
                        )
                    },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(12.dp),

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor =
                            NeonCyan.copy(alpha = 0.3f),

                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,

                        cursorColor = NeonCyan
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,

                    onValueChange = {
                        password = it
                        passwordError = null
                    },

                    label = {
                        Text(
                            "Пароль",
                            color = NeonPurple
                        )
                    },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(12.dp),

                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor =
                            NeonPurple.copy(alpha = 0.3f),

                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,

                        cursorColor = NeonPurple,
                        errorBorderColor = Color.Red
                    ),

                    isError = passwordError != null,

                    supportingText = passwordError?.let {
                        {
                            Text(
                                it,
                                color = Color.Red
                            )
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {

                        when {

                            name.isBlank() -> {
                                passwordError =
                                    "Введите имя"
                            }

                            password.length < 6 -> {
                                passwordError =
                                    "Минимум 6 символов"
                            }

                            else -> {
                                onConfirm(
                                    name.trim(),
                                    password
                                )
                            }
                        }
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple
                    ),

                    shape = RoundedCornerShape(16.dp)
                ) {

                    Text(
                        "Создать аккаунт",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        "Отмена",
                        color = Color.Gray
                    )
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
    onSetupAnonymous: (String, String) -> Unit,
    onOpenDialog: () -> Unit,
    onLaunchTyr: () -> Unit
) {

    val infiniteTransition =
        rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,

        animationSpec = infiniteRepeatable(
            animation = tween(
                2000,
                easing = EaseInOutCubic
            ),

            repeatMode = RepeatMode.Reverse
        ),

        label = "scale"
    )

    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,

        animationSpec = infiniteRepeatable(
            animation = tween(
                4000,
                easing = LinearEasing
            ),

            repeatMode = RepeatMode.Reverse
        ),

        label = "gradient"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        DeepPurple,
                        Color(0xFF0D0020)
                            .copy(alpha = 0.9f),
                        DarkBackground
                    ),

                    startY = 0f + gradientShift * 200f,
                    endY = 1000f + gradientShift * 200f
                )
            )
    ) {

        Box(
            modifier = Modifier
                .size(300.dp)
                .offset((-100).dp, (-50).dp)
                .clip(CircleShape)
                .background(
                    NeonPurple.copy(alpha = 0.05f)
                )
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(250.dp, 600.dp)
                .clip(CircleShape)
                .background(
                    NeonCyan.copy(alpha = 0.05f)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(
                    horizontal = 32.dp,
                    vertical = 24.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.weight(0.3f))

            Image(
                painter = painterResource(id = R.drawable.intro1),
                contentDescription = "Логотип",

                modifier = Modifier
                    .size(200.dp)
                    .scale(scale)
            )

            Spacer(modifier = Modifier.weight(0.5f))

            if (isLoading) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                NeonPurple.copy(alpha = 0.2f)
                            )
                            .border(
                                3.dp,
                                NeonPurple,
                                CircleShape
                            ),

                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏳", fontSize = 32.sp)
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        loadingMessage,
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(200.dp)
                            .clip(
                                RoundedCornerShape(4.dp)
                            ),

                        color = NeonPurple,

                        trackColor =
                            NeonPurple.copy(alpha = 0.2f)
                    )
                }

            } else {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Button(
                        onClick = onLaunchEmail,

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                NeonCyan.copy(alpha = 0.15f)
                        ),

                        shape = RoundedCornerShape(20.dp),

                        border = BorderStroke(
                            2.dp,
                            NeonCyan.copy(alpha = 0.5f)
                        )
                    ) {

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    NeonCyan.copy(alpha = 0.2f)
                                )
                                .border(
                                    1.dp,
                                    NeonCyan,
                                    CircleShape
                                ),

                            contentAlignment = Alignment.Center
                        ) {
                            Text("📧", fontSize = 18.sp)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            Modifier.weight(1f)
                        ) {

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

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onOpenDialog,

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                NeonPurple.copy(alpha = 0.15f)
                        ),

                        shape = RoundedCornerShape(20.dp),

                        border = BorderStroke(
                            2.dp,
                            NeonPurple.copy(alpha = 0.5f)
                        )
                    ) {

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    NeonPurple.copy(alpha = 0.2f)
                                )
                                .border(
                                    1.dp,
                                    NeonPurple,
                                    CircleShape
                                ),

                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛡️", fontSize = 18.sp)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            Modifier.weight(1f)
                        ) {

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
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            Row(
                modifier = Modifier
                    .clickable {
                        onLaunchTyr()
                    }
                    .padding(8.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Настройки",

                    tint = Color.Gray.copy(alpha = 0.3f),

                    modifier = Modifier.size(16.dp)
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    "⚙️",
                    fontSize = 16.sp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )
            }
        }
    }
}

и 

// Внутри setContent вашего MainActivity:

NavGraph(
    navController = navController,
    startDestination = Routes.CHATS,
    chatLayer = {
        if (isRegistered.value) {
            AndroidView(
                factory = {
                    if (chatContainer == null) {
                        initChatLayer()
                    }
                    chatContainer!!
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Принудительно ставим видимость, если нужно
                    view.visibility = View.VISIBLE
                }
            )
        }
    }
)
