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

    val backStackEntry by
        navController.currentBackStackEntryAsState()

    val currentRoute =
        backStackEntry?.destination?.route

    val showBottomBar =
        currentRoute in listOf(
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

        // КРИТИЧНО:
        // прозрачный scaffold на экране чатов
        containerColor =
            if (currentRoute == Routes.CHATS) {
                Color.Transparent
            } else {
                AppBlack
            }

    ) { paddingValues ->

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            // ====================================================
            // NATIVE DELTACHAT LAYER
            // ====================================================
            // ВАЖНО:
            // НЕ удаляем layer из composition.
            // Fragment должен жить всегда.
            // Меняем visibility в MainActivity.
            // ====================================================

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                chatLayer()
            }

            // ====================================================
            // COMPOSE NAVIGATION OVERLAY
            // ====================================================

            NavHost(
                navController = navController,

                startDestination = startDestination,

                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .background(
                        if (currentRoute == Routes.CHATS) {
                            Color.Transparent
                        } else {
                            AppBlack
                        }
                    )
            ) {

                // ====================================================
                // CHATS
                // ====================================================

                composable(Routes.CHATS) {

                    // Пустой прозрачный overlay
                    // над native DeltaChat fragment
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }

                // ====================================================
                // DEALS
                // ====================================================

                composable(Routes.DEALS) {

                    ScreenContainer(
                        paddingValues = paddingValues
                    ) {

                        DealsScreen(navController)
                    }
                }

                // ====================================================
                // ENTERTAINMENT
                // ====================================================

                composable(Routes.ENTERTAINMENT) {

                    ScreenContainer(
                        paddingValues = paddingValues
                    ) {

                        EntertainmentScreen(navController)
                    }
                }

                // ====================================================
                // SETTINGS
                // ====================================================

                composable(Routes.SETTINGS) {

                    ScreenContainer(
                        paddingValues = paddingValues
                    ) {

                        SettingsScreen(navController)
                    }
                }

                // ====================================================
                // MUSIC
                // ====================================================

                composable(Routes.MUSIC) {

                    ScreenContainer {

                        MusicPlayerScreen()
                    }
                }

                // ====================================================
                // WEBVIEW
                // ====================================================

                composable(
                    route = "webview/{url}/{title}",

                    arguments = listOf(

                        navArgument("url") {
                            type = NavType.StringType
                        },

                        navArgument("title") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->

                    val url =
                        entry.arguments?.getString("url")
                            ?: ""

                    val title =
                        entry.arguments?.getString("title")
                            ?: ""

                    ScreenContainer {

                        if (isOnline) {

                            WebViewScreen(
                                url = url,
                                title = title,
                                navController = navController
                            )

                        } else {

                            NoInternetScreen(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }

                // ====================================================
                // CALCULATOR
                // ====================================================

                composable(Routes.CALCULATOR) {

                    ScreenContainer {

                        CalculatorScreen()
                    }
                }

                // ====================================================
                // TEXT EDITOR
                // ====================================================

                composable(Routes.TEXT_EDITOR) {

                    ScreenContainer {

                        TextEditorScreen(navController)
                    }
                }

                // ====================================================
                // FILE MANAGER
                // ====================================================

                composable(Routes.FILE_MANAGER) {

                    ScreenContainer {

                        FileManagerScreen(
                            onExit = {
                                navController.popBackStack()
                            }
                        )
                    }
                }

                // ====================================================
                // TIC TAC TOE
                // ====================================================

                composable(Routes.TIC_TAC_TOE) {

                    ScreenContainer {

                        TicTacToeScreen()
                    }
                }

                // ====================================================
                // CHESS
                // ====================================================

                composable(Routes.CHESS) {

                    ScreenContainer {

                        ChessScreen()
                    }
                }

                // ====================================================
                // PACMAN
                // ====================================================

                composable(Routes.PACMAN) {

                    ScreenContainer {

                        PacmanScreen()
                    }
                }

                // ====================================================
                // SUDOKU
                // ====================================================

                composable(Routes.SUDOKU) {

                    ScreenContainer {

                        SudokuScreen()
                    }
                }

                // ====================================================
                // JEWELS
                // ====================================================

                composable(Routes.JEWELS) {

                    ScreenContainer {

                        JewelsBlastScreen()
                    }
                }

                // ====================================================
                // AI CHAT
                // ====================================================

                composable(Routes.AI_CHAT) {

                    ScreenContainer {

                        AiChatScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenContainer(
    paddingValues: PaddingValues = PaddingValues(),
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
        tonalElevation = 0.dp
    ) {

        val items = listOf(

            Triple(
                Routes.CHATS,
                Icons.Outlined.ChatBubbleOutline,
                "Чаты"
            ),

            Triple(
                Routes.DEALS,
                Icons.Filled.Checklist,
                "Дела"
            ),

            Triple(
                Routes.ENTERTAINMENT,
                Icons.Outlined.PlayCircleOutline,
                "Досуг"
            ),

            Triple(
                Routes.SETTINGS,
                Icons.Filled.Settings,
                "Опции"
            )
        )

        items.forEach { (route, icon, label) ->

            val selected =
                currentRoute == route

            NavigationBarItem(

                selected = selected,

                onClick = {

                    if (!selected) {

                        navController.navigate(route) {

                            popUpTo(
                                navController.graph.startDestinationId
                            ) {
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

                        tint =
                            if (selected) {
                                NeonCyan
                            } else {
                                Color.Gray
                            }
                    )
                },

                label = {

                    Text(
                        text = label,

                        fontSize = 10.sp,

                        color =
                            if (selected) {
                                NeonCyan
                            } else {
                                Color.Gray
                            }
                    )
                },

                colors = NavigationBarItemDefaults.colors(
                    indicatorColor =
                        NeonCyan.copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
fun rememberIsOnline(): State<Boolean> {

    val context = LocalContext.current

    val state = remember {
        mutableStateOf(true)
    }

    DisposableEffect(context) {

        val connectivityManager =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val callback =
            object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {
                    state.value = true
                }

                override fun onLost(network: Network) {
                    state.value =
                        hasInternetConnection(connectivityManager)
                }

                override fun onUnavailable() {
                    state.value = false
                }
            }

        try {

            val request =
                NetworkRequest.Builder()
                    .addCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )
                    .build()

            connectivityManager.registerNetworkCallback(
                request,
                callback
            )

            state.value =
                hasInternetConnection(connectivityManager)

        } catch (_: Exception) {

            state.value = true
        }

        onDispose {

            try {

                connectivityManager.unregisterNetworkCallback(
                    callback
                )

            } catch (_: Exception) {
            }
        }
    }

    return state
}

private fun hasInternetConnection(
    connectivityManager: ConnectivityManager
): Boolean {

    return try {

        val network =
            connectivityManager.activeNetwork
                ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )

    } catch (_: Exception) {

        true
    }
}

@Composable
fun NoInternetScreen(
    onBack: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack),

        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center,

            modifier = Modifier.padding(24.dp)
        ) {

            Icon(
                imageVector = Icons.Default.CloudOff,

                contentDescription = null,

                tint = NeonCyan.copy(alpha = 0.6f),

                modifier = Modifier.size(80.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Нет соединения",

                color = Color.White,

                fontSize = 22.sp,

                fontWeight = FontWeight.Bold,

                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Проверьте интернет и попробуйте снова",

                color = Color.Gray,

                fontSize = 14.sp,

                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = onBack,

                border = BorderStroke(
                    1.dp,
                    NeonCyan
                ),

                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonCyan
                    )
            ) {

                Text(
                    text = "ВЕРНУТЬСЯ",

                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
