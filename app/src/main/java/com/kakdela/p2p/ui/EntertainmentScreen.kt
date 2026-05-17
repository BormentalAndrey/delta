package com.kakdela.p2p.ui

import androidx.compose.foundation.border
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.launcher.multiapp.R
import com.kakdela.p2p.ui.navigation.Routes
import com.kakdela.p2p.ui.player.MusicManager
import com.kakdela.p2p.ui.player.VideoPlayerActivity
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Неоновая киберпанк-палитра
private val NeonGreen = Color(0xFF00FF41)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonYellow = Color(0xFFFFD700)
private val NeonPurple = Color(0xFFBA00FF)
private val NeonPink = Color(0xFFFF1493)
private val NeonOrange = Color(0xFFFF6600)
private val NeonBlue = Color(0xFF0088FF)
private val NeonRed = Color(0xFFFF0040)
private val NeonLime = Color(0xFFCCFF00)
private val NeonTeal = Color(0xFF00FFCC)
private val NeonGold = Color(0xFFFFAA00)

enum class EntertainmentType {
    WEB,
    INTERNAL_CHAT,
    GAME,
    MUSIC,
    VIDEO
}

data class EntertainmentItem(
    val id: String,
    val title: String,
    val description: String,
    val type: EntertainmentType,
    val route: String? = null,
    val url: String? = null,
    val accentColor: Color? = null,
    val iconVector: ImageVector? = null
)

private val entertainmentItems = listOf(
    EntertainmentItem(
        id = "music",
        title = "Музыка",
        description = "MP3 проигрыватель",
        type = EntertainmentType.MUSIC,
        route = Routes.MUSIC,
        accentColor = NeonYellow,
        iconVector = Icons.Filled.MusicNote
    ),
    EntertainmentItem(
        id = "video",
        title = "Видео",
        description = "Неоновый видео плеер",
        type = EntertainmentType.VIDEO,
        accentColor = NeonPurple,
        iconVector = Icons.Filled.Movie
    ),
    EntertainmentItem(
        id = "ai_chat",
        title = "AI Чат",
        description = "Умный помощник",
        type = EntertainmentType.INTERNAL_CHAT,
        route = Routes.AI_CHAT,
        accentColor = NeonCyan,
        iconVector = Icons.Filled.Chat
    ),
    EntertainmentItem(
        id = "tictactoe",
        title = "Крестики-нолики",
        description = "Игра против ИИ",
        type = EntertainmentType.GAME,
        route = Routes.TIC_TAC_TOE,
        accentColor = NeonGreen,
        iconVector = Icons.Filled.Gamepad
    ),
    EntertainmentItem(
        id = "pacman",
        title = "Pacman",
        description = "Классическая аркада",
        type = EntertainmentType.GAME,
        route = Routes.PACMAN,
        accentColor = NeonYellow,
        iconVector = Icons.Filled.SportsEsports
    ),
    EntertainmentItem(
        id = "jewels",
        title = "Кристаллы",
        description = "Три в ряд",
        type = EntertainmentType.GAME,
        route = Routes.JEWELS,
        accentColor = NeonPink,
        iconVector = Icons.Filled.Diamond
    ),
    EntertainmentItem(
        id = "sudoku",
        title = "Судоку",
        description = "Головоломка 9x9",
        type = EntertainmentType.GAME,
        route = Routes.SUDOKU,
        accentColor = NeonOrange,
        iconVector = Icons.Filled.GridOn
    ),
    EntertainmentItem(
        id = "chess",
        title = "Шахматы",
        description = "Игра против ИИ",
        type = EntertainmentType.GAME,
        route = Routes.CHESS,
        accentColor = NeonGold,
        iconVector = Icons.Filled.Casino
    ),
    EntertainmentItem(
        id = "tiktok",
        title = "TikTok",
        description = "Смотреть (ПК режим)",
        type = EntertainmentType.WEB,
        url = "https://www.tiktok.com",
        accentColor = NeonPink,
        iconVector = Icons.Filled.MusicVideo
    ),
    EntertainmentItem(
        id = "pikabu",
        title = "Пикабу",
        description = "Юмор",
        type = EntertainmentType.WEB,
        url = "https://pikabu.ru",
        accentColor = NeonOrange,
        iconVector = Icons.Filled.SentimentVerySatisfied
    ),
    EntertainmentItem(
        id = "vk",
        title = "ВКонтакте",
        description = "Соц.сеть",
        type = EntertainmentType.WEB,
        url = "https://vk.com",
        accentColor = NeonBlue,
        iconVector = Icons.Filled.People
    ),
    EntertainmentItem(
        id = "crazygames",
        title = "CrazyGames",
        description = "Игры онлайн",
        type = EntertainmentType.WEB,
        url = "https://www.crazygames.com",
        accentColor = NeonLime,
        iconVector = Icons.Filled.VideogameAsset
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntertainmentScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Развлечения",
                        fontWeight = FontWeight.Black,
                        color = NeonGreen,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(entertainmentItems, key = { it.id }) { item ->
                EntertainmentNeonItem(item, navController)
            }
        }
    }
}

@Composable
fun EntertainmentNeonItem(
    item: EntertainmentItem,
    navController: NavHostController
) {
    val context = LocalContext.current
    val neonColor = item.accentColor ?: when (item.type) {
        EntertainmentType.GAME -> NeonGreen
        EntertainmentType.INTERNAL_CHAT -> NeonCyan
        EntertainmentType.WEB -> NeonMagenta
        EntertainmentType.MUSIC -> NeonYellow
        EntertainmentType.VIDEO -> NeonPurple
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .shadow(12.dp, spotColor = neonColor, ambientColor = neonColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, neonColor.copy(alpha = 0.7f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        onClick = {
            val route = item.route
            val url = item.url

            when {
                item.type == EntertainmentType.VIDEO -> {
                    context.startActivity(Intent(context, VideoPlayerActivity::class.java))
                }
                !url.isNullOrBlank() -> {
                    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    val encodedTitle = URLEncoder.encode(item.title, StandardCharsets.UTF_8.toString())
                    navController.navigate("webview?url=$encodedUrl&title=$encodedTitle")
                }
                !route.isNullOrBlank() && (route.startsWith("http://") || route.startsWith("https://")) -> {
                    val encodedUrl = URLEncoder.encode(route, StandardCharsets.UTF_8.toString())
                    val encodedTitle = URLEncoder.encode(item.title, StandardCharsets.UTF_8.toString())
                    navController.navigate("webview?url=$encodedUrl&title=$encodedTitle")
                }
                !route.isNullOrBlank() -> {
                    navController.navigate(route)
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(neonColor.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка с неоновой подложкой
            if (item.type == EntertainmentType.MUSIC && MusicManager.currentTrack != null) {
                AsyncImage(
                    model = MusicManager.currentTrack!!.albumArt,
                    contentDescription = null,
                    error = painterResource(id = R.drawable.ic_music_placeholder),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, neonColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(neonColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .border(1.dp, neonColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.iconVector ?: when (item.type) {
                            EntertainmentType.GAME -> Icons.Filled.Gamepad
                            EntertainmentType.INTERNAL_CHAT -> Icons.Filled.Chat
                            EntertainmentType.WEB -> Icons.Filled.Public
                            EntertainmentType.MUSIC -> Icons.Filled.MusicNote
                            EntertainmentType.VIDEO -> Icons.Filled.Movie
                        },
                        contentDescription = null,
                        tint = neonColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                if (item.type == EntertainmentType.MUSIC && MusicManager.currentTrack != null) {
                    Text(
                        text = MusicManager.currentTrack!!.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Сейчас играет",
                        color = neonColor.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                } else {
                    Text(
                        text = item.title.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = item.description,
                        color = neonColor.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            if (item.type == EntertainmentType.MUSIC) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { MusicManager.playPrevious(context) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.ChevronLeft, null, tint = neonColor)
                    }
                    IconButton(
                        onClick = {
                            if (MusicManager.currentIndex == -1)
                                MusicManager.playTrack(context, 0)
                            else
                                MusicManager.togglePlayPause()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (MusicManager.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = neonColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = { MusicManager.playNext(context) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.ChevronRight, null, tint = neonColor)
                    }
                }
            } else {
                Icon(
                    Icons.Filled.PlayArrow,
                    null,
                    tint = neonColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
