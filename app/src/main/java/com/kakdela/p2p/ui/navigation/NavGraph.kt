package com.kakdela.p2p.ui.navigation

import android.view.ViewGroup
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.mms.GlideApp
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.database.Address
import com.b44t.messenger.DcContact
import org.thoughtcrime.securesms.accounts.AccountSelectionListFragment

// Глобально сохраняем rootView DeltaChat, чтобы он не уничтожался при переключении вкладок
private var globalChatRootView: FrameLayout? = null
private var globalChatInitialized = false

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
                .padding(paddingValues)
                .background(Color.Black)
        ) {

            composable(Routes.CHATS) {
                DeltaChatLayoutView()
            }

            composable(Routes.DEALS) { DealsScreen(navController) }
            composable(Routes.ENTERTAINMENT) { EntertainmentScreen(navController) }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }
            composable(Routes.MUSIC) { MusicPlayerScreen() }

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

            composable(Routes.CALCULATOR) { CalculatorScreen() }
            composable(Routes.TEXT_EDITOR) { TextEditorScreen(navController) }
            composable(Routes.FILE_MANAGER) { FileManagerScreen(onExit = { navController.popBackStack() }) }
            composable(Routes.TIC_TAC_TOE) { TicTacToeScreen() }
            composable(Routes.CHESS) { ChessScreen() }
            composable(Routes.PACMAN) { PacmanScreen() }
            composable(Routes.SUDOKU) { SudokuScreen() }
            composable(Routes.JEWELS) { JewelsBlastScreen() }
            composable(Routes.AI_CHAT) { AiChatScreen() }
        }
    }
}

@Composable
fun DeltaChatLayoutView() {

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            // Если уже создан глобально — возвращаем его
            if (globalChatRootView != null) {
                return@AndroidView globalChatRootView!!
            }

            val rootView = FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val pkg = ctx.packageName
            val layoutId = ctx.resources.getIdentifier("conversation_list_activity", "layout", pkg)

            val inflater = LayoutInflater.from(ctx)
            val deltaLayout = inflater.inflate(layoutId, rootView, false)
            rootView.addView(deltaLayout)

            // Toolbar
            val toolbarId = ctx.resources.getIdentifier("toolbar", "id", pkg)
            if (toolbarId != 0) {
                val toolbar = deltaLayout.findViewById<Toolbar>(toolbarId)
                if (ctx is AppCompatActivity && (ctx as AppCompatActivity).supportActionBar == null) {
                    ctx.setSupportActionBar(toolbar)
                }
            }

            // Fragment — только один раз
            val fragContainerId = ctx.resources.getIdentifier("fragment_container", "id", pkg)
            if (fragContainerId != 0) {
                val fragmentContainer = deltaLayout.findViewById<FrameLayout>(fragContainerId)
                if (fragmentContainer != null) {
                    val fragmentManager = (ctx as FragmentActivity).supportFragmentManager
                    if (fragmentManager.findFragmentById(fragContainerId) == null) {
                        val fragment = ConversationListFragment()
                        fragment.arguments = Bundle().apply {
                            putBoolean(ConversationListFragment.ARCHIVE, false)
                        }
                        fragmentManager.beginTransaction()
                            .replace(fragContainerId, fragment)
                            .commit()
                    }
                }
            }

            // Поиск
            val searchActionId = ctx.resources.getIdentifier("search_action", "id", pkg)
            val searchToolbarId = ctx.resources.getIdentifier("search_toolbar", "id", pkg)
            if (searchActionId != 0 && searchToolbarId != 0) {
                val searchAction = deltaLayout.findViewById<android.widget.ImageView>(searchActionId)
                val searchToolbar = deltaLayout.findViewById<org.thoughtcrime.securesms.components.SearchToolbar>(searchToolbarId)
                searchAction?.setOnClickListener { view ->
                    searchToolbar?.display(view.x.toFloat(), view.y.toFloat())
                }
            }

            // Аватар
            val selfAvatarId = ctx.resources.getIdentifier("self_avatar", "id", pkg)
            if (selfAvatarId != 0) {
                val selfAvatar = deltaLayout.findViewById<org.thoughtcrime.securesms.components.AvatarView>(selfAvatarId)
                selfAvatar?.setOnClickListener {
                    if (ctx is FragmentActivity) {
                        AccountSelectionListFragment.newInstance(false)
                            .show(ctx.supportFragmentManager, null)
                    }
                }
            }

            globalChatRootView = rootView
            rootView
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
                        icon,
                        contentDescription = label,
                        tint = if (selected) Color(0xFF00FFFF) else Color.Gray
                    )
                },
                label = {
                    Text(
                        label,
                        fontSize = 10.sp,
                        color = if (selected) Color(0xFF00FFFF) else Color.Gray
                    )
                }
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
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            state.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (_: Exception) {
            state.value = true
        }
        onDispose {
            try { cm.unregisterNetworkCallback(callback) } catch (_: Exception) {}
        }
    }

    return state
}

@Composable
fun NoInternetScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = Color(0xFF00FFFF).copy(alpha = 0.6f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text("Нет соединения", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, Color(0xFF00FFFF)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FFFF))
            ) {
                Text("ВЕРНУТЬСЯ", fontWeight = FontWeight.Bold)
            }
        }
    }
}
