package com.kakdela.p2p.ui.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.kakdela.p2p.ui.*
import com.kakdela.p2p.ui.chat.AiChatScreen
import com.kakdela.p2p.ui.player.MusicPlayerScreen
import com.kakdela.p2p.ui.screens.FileManagerScreen
import org.thoughtcrime.securesms.ConversationListFragment

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val isOnline by rememberIsOnline()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Routes.CHATS,
        Routes.DEALS,
        Routes.ENTERTAINMENT,
        Routes.SETTINGS
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(currentRoute, navController)
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- ВКЛАДКА ЧАТЫ (Интеграция Delta Chat) ---
            composable(Routes.CHATS) {
                DeltaChatView(Modifier.fillMaxSize())
            }

            // --- ВКЛАДКА ДЕЛА ---
            composable(Routes.DEALS) {
                DealsScreen(navController)
            }

            // --- ВКЛАДКА ДОСУГ ---
            composable(Routes.ENTERTAINMENT) {
                EntertainmentScreen(navController)
            }

            // --- ВКЛАДКА ОПЦИИ ---
            composable(Routes.SETTINGS) {
                SettingsScreen(navController)
            }

            // --- ДОПОЛНИТЕЛЬНЫЕ ЭКРАНЫ ---
            composable(Routes.MUSIC) { MusicPlayerScreen() }
            composable(Routes.CALCULATOR) { CalculatorScreen() }
            composable(Routes.TEXT_EDITOR) { TextEditorScreen(navController) }
            composable(Routes.AI_CHAT) { AiChatScreen() }
            composable(Routes.FILE_MANAGER) { 
                FileManagerScreen(onExit = { navController.popBackStack() }) 
            }
            
            // Игры
            composable(Routes.TIC_TAC_TOE) { TicTacToeScreen() }
            composable(Routes.CHESS) { ChessScreen() }
            composable(Routes.PACMAN) { PacmanScreen() }
            composable(Routes.SUDOKU) { SudokuScreen() }
            composable(Routes.JEWELS) { JewelsBlastScreen() }

            // WebView
            composable(
                route = "webview/{url}/{title}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { entry ->
                val url = entry.arguments?.getString("url") ?: ""
                val title = entry.arguments?.getString("title") ?: ""
                if (isOnline) {
                    WebViewScreen(url, title, navController)
                } else {
                    NoInternetScreen { navController.popBackStack() }
                }
            }
        }
    }
}

/**
 * Встраивает нативный фрагмент списка чатов Delta Chat в Compose.
 */
@Composable
fun DeltaChatView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            // Создаем контейнер с уникальным ID для фрагмента
            FrameLayout(ctx).apply {
                id = View.generateViewId()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { frameLayout ->
            val activity = frameLayout.context as? FragmentActivity
            activity?.supportFragmentManager?.let { fragmentManager ->
                // Проверяем, не добавлен ли фрагмент уже, чтобы не пересоздавать его при каждой рекомпозиции
                if (fragmentManager.findFragmentById(frameLayout.id) == null) {
                    val fragment = ConversationListFragment().apply {
                        arguments = Bundle().apply {
                            putBoolean(ConversationListFragment.ARCHIVE, false)
                        }
                    }
                    fragmentManager.beginTransaction()
                        .replace(frameLayout.id, fragment)
                        .commitNowAllowingStateLoss()
                }
            }
        }
    )
}

@Composable
private fun AppBottomBar(
    currentRoute: String?,
    navController: NavHostController
) {
    NavigationBar(
        containerColor = Color(0xFF010101),
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            Triple(Routes.CHATS, Icons.Outlined.ChatBubbleOutline, "Чаты"),
            Triple(Routes.DEALS, Icons.Filled.Checklist, "Дела"),
            Triple(Routes.ENTERTAINMENT, Icons.Outlined.PlayCircleOutline, "Досуг"),
            Triple(Routes.SETTINGS, Icons.Filled.Settings, "Опции")
        )

        items.forEach { (route, icon, label) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) Color(0xFF00FFFF) else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = if (selected) Color(0xFF00FFFF) else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun rememberIsOnline(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(true) }

    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { state.value = true }
            override fun onLost(network: Network) { state.value = false }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        cm.registerNetworkCallback(request, callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return state
}

@Composable
fun NoInternetScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = Color(0xFF00FFFF).copy(alpha = 0.6f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text("Нет соединения", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, Color(0xFF00FFFF)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FFFF))
            ) {
                Text("ВЕРНУТЬСЯ")
            }
        }
    }
}
