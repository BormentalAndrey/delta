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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    chatLayer: @Composable () -> Unit
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
        containerColor = Color.Black,
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    navController = navController
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
            ) {
                // ================= CHATS =================
                composable(Routes.CHATS) {
                    chatLayer()
                }

                // ================= DEALS =================
                composable(Routes.DEALS) {
                    DealsScreen(navController)
                }

                // ================= ENTERTAINMENT =================
                composable(Routes.ENTERTAINMENT) {
                    EntertainmentScreen(navController)
                }

                // ================= SETTINGS =================
                composable(Routes.SETTINGS) {
                    SettingsScreen(navController)
                }

                // ================= TOOLS =================
                composable(Routes.MUSIC) { MusicPlayerScreen() }
                composable(Routes.CALCULATOR) { CalculatorScreen() }
                composable(Routes.TEXT_EDITOR) { TextEditorScreen(navController) }
                composable(Routes.AI_CHAT) { AiChatScreen() }
                composable(Routes.FILE_MANAGER) {
                    FileManagerScreen(onExit = { navController.popBackStack() })
                }

                // ================= GAMES =================
                composable(Routes.TIC_TAC_TOE) { TicTacToeScreen() }
                composable(Routes.CHESS) { ChessScreen() }
                composable(Routes.PACMAN) { PacmanScreen() }
                composable(Routes.SUDOKU) { SudokuScreen() }
                composable(Routes.JEWELS) { JewelsBlastScreen() }

                // ================= WEBVIEW =================
                // Использование Query параметров предотвращает краши из-за слэшей в URL
                composable(
                    route = "webview?url={url}&title={title}",
                    arguments = listOf(
                        navArgument("url") { 
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("title") { 
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { entry ->
                    val encodedUrl = entry.arguments?.getString("url") ?: ""
                    val encodedTitle = entry.arguments?.getString("title") ?: ""
                    
                    val url = try { URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.name()) } catch (e: Exception) { encodedUrl }
                    val title = try { URLDecoder.decode(encodedTitle, StandardCharsets.UTF_8.name()) } catch (e: Exception) { encodedTitle }

                    if (isOnline) {
                        WebViewScreen(url, title, navController)
                    } else {
                        NoInternetScreen { navController.popBackStack() }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    currentRoute: String?,
    navController: NavHostController
) {
    val items = remember {
        listOf(
            Triple(Routes.CHATS, Icons.Outlined.ChatBubbleOutline, "Чаты"),
            Triple(Routes.DEALS, Icons.Filled.Checklist, "Дела"),
            Triple(Routes.ENTERTAINMENT, Icons.Outlined.PlayCircleOutline, "Досуг"),
            Triple(Routes.SETTINGS, Icons.Filled.Settings, "Опции")
        )
    }

    NavigationBar(
        containerColor = Color(0xFF010101),
        tonalElevation = 0.dp
    ) {
        items.forEach { (route, icon, label) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(route) {
                            // Правильный метод для нахождения стартового экрана графа
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
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
    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    val state = remember { 
        mutableStateOf(
            connectivityManager.activeNetwork?.let { network ->
                connectivityManager.getNetworkCapabilities(network)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } ?: false
        ) 
    }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                state.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }

            override fun onAvailable(network: Network) {
                val caps = connectivityManager.getNetworkCapabilities(network)
                state.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
            }

            override fun onLost(network: Network) {
                state.value = false
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        onDispose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }

    return state
}

@Composable
fun NoInternetScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = Color(0xFF00FFFF).copy(alpha = 0.6f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Нет соединения",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
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
