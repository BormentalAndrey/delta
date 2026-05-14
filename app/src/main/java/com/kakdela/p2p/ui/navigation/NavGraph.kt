package com.kakdela.p2p.ui.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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

private val NeonCyan = Color(0xFF00FFFF)
private val AppBlack = Color.Black
private val BottomBarBlack = Color(0xFF010101)

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    chatLayer: @Composable () -> Unit
) {
    val isOnline by rememberIsOnline()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Определяем, на какой вкладке показывать BottomBar
    val showBottomBar = currentRoute in listOf(
        Routes.CHATS,
        Routes.DEALS,
        Routes.ENTERTAINMENT,
        Routes.SETTINGS
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    navController = navController
                )
            }
        },
        // Делаем фон Scaffold прозрачным только для экрана чатов
        containerColor = if (currentRoute == Routes.CHATS) Color.Transparent else AppBlack
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {
            
            // --- СЛОЙ 0: НАТИВНЫЙ (Delta Chat Fragment) ---
            // Он всегда в дереве, чтобы не терять состояние фрагмента.
            // Padding применяется только снизу, чтобы контент не заходил под BottomBar.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showBottomBar) paddingValues.calculateBottomPadding() else 0.dp)
            ) {
                chatLayer()
            }

            // --- СЛОЙ 1: COMPOSE NAVIGATION ---
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f) // Поверх нативного слоя
                    .background(
                        if (currentRoute == Routes.CHATS) Color.Transparent else AppBlack
                    )
            ) {
                // Главные экраны
                composable(Routes.CHATS) {
                    // Оставляем пустым, чтобы видеть chatLayer через прозрачный фон NavHost
                    Box(modifier = Modifier.fillMaxSize())
                }

                composable(Routes.DEALS) {
                    ScreenContainer(paddingValues) { DealsScreen(navController) }
                }

                composable(Routes.ENTERTAINMENT) {
                    ScreenContainer(paddingValues) { EntertainmentScreen(navController) }
                }

                composable(Routes.SETTINGS) {
                    ScreenContainer(paddingValues) { SettingsScreen(navController) }
                }

                // Инструменты и Досуг (Без нижнего бара)
                composable(Routes.MUSIC) {
                    ScreenContainer { MusicPlayerScreen() }
                }

                composable(Routes.CALCULATOR) {
                    ScreenContainer { CalculatorScreen() }
                }

                composable(Routes.TEXT_EDITOR) {
                    ScreenContainer { TextEditorScreen(navController) }
                }

                composable(Routes.AI_CHAT) {
                    ScreenContainer { AiChatScreen() }
                }

                composable(Routes.FILE_MANAGER) {
                    ScreenContainer { 
                        FileManagerScreen(onExit = { navController.popBackStack() }) 
                    }
                }

                // Игры
                composable(Routes.TIC_TAC_TOE) { ScreenContainer { TicTacToeScreen() } }
                composable(Routes.CHESS) { ScreenContainer { ChessScreen() } }
                composable(Routes.PACMAN) { ScreenContainer { PacmanScreen() } }
                composable(Routes.JEWELS) { ScreenContainer { JewelsBlastScreen() } }
                composable(Routes.SUDOKU) { ScreenContainer { SudokuScreen() } }

                // WebView с обработкой интернета
                composable(
                    route = "webview/{url}/{title}",
                    arguments = listOf(
                        navArgument("url") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType }
                    )
                ) { entry ->
                    val url = entry.arguments?.getString("url") ?: ""
                    val title = entry.arguments?.getString("title") ?: ""

                    ScreenContainer {
                        if (isOnline) {
                            WebViewScreen(url, title, navController)
                        } else {
                            NoInternetScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenContainer(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(AppBlack)
    ) {
        content()
    }
}

@Composable
private fun AppBottomBar(
    currentRoute: String?,
    navController: NavHostController
) {
    NavigationBar(
        containerColor = BottomBarBlack,
        tonalElevation = 0.dp,
        modifier = Modifier.height(72.dp)
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
                        tint = if (selected) NeonCyan else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = if (selected) NeonCyan else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = NeonCyan.copy(alpha = 0.1f)
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
            override fun onLost(network: Network) { state.value = checkConnectivity(cm) }
            override fun onUnavailable() { state.value = false }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        cm.registerNetworkCallback(request, callback)
        state.value = checkConnectivity(cm)

        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return state
}

private fun checkConnectivity(cm: ConnectivityManager): Boolean {
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun NoInternetScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(AppBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CloudOff, null, tint = NeonCyan.copy(0.6f), modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(24.dp))
            Text("Нет соединения", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, NeonCyan),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
            ) {
                Text("ВЕРНУТЬСЯ")
            }
        }
    }
}
