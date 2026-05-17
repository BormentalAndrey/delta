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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kakdela.p2p.ui.browser.BrowserActivity
import com.kakdela.p2p.ui.TransferActivity
import com.kakdela.p2p.ui.navigation.Routes
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Неоновая киберпанк-палитра
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonGreen = Color(0xFF00FF41)
private val NeonYellow = Color(0xFFFFD700)
private val NeonOrange = Color(0xFFFF6600)
private val NeonPink = Color(0xFFFF1493)
private val NeonBlue = Color(0xFF0088FF)
private val NeonPurple = Color(0xFFB026FF)
private val NeonRed = Color(0xFFFF0040)
private val NeonLime = Color(0xFFCCFF00)
private val NeonTeal = Color(0xFF00FFCC)

enum class DealType { WEB, CALCULATOR, TOOL, ACTIVITY }

data class DealItem(
    val id: String,
    val title: String,
    val description: String,
    val type: DealType,
    val url: String? = null,
    val accentColor: Color = NeonMagenta,
    val iconVector: ImageVector? = null
)

private val dealItems = listOf(
    DealItem(
        id = "browser",
        title = "P2P Браузер",
        description = "Анонимный сёрфинг",
        type = DealType.ACTIVITY,
        accentColor = NeonCyan,
        iconVector = Icons.Filled.Public
    ),
    DealItem(
        id = "transfer",
        title = "P2P Обмен файлов",
        description = "Передача без интернета",
        type = DealType.ACTIVITY,
        accentColor = NeonGreen,
        iconVector = Icons.Filled.Share
    ),
    DealItem(
        id = "file_manager",
        title = "Файловый менеджер",
        description = "Управление файлами",
        type = DealType.TOOL,
        accentColor = NeonYellow,
        iconVector = Icons.Filled.Folder
    ),
    DealItem(
        id = "calculator",
        title = "Калькулятор",
        description = "Быстрые расчёты",
        type = DealType.CALCULATOR,
        accentColor = NeonOrange,
        iconVector = Icons.Filled.Calculate
    ),
    DealItem(
        id = "text_editor",
        title = "Текстовый редактор",
        description = "TXT, DOCX, PDF",
        type = DealType.TOOL,
        accentColor = NeonPink,
        iconVector = Icons.Filled.Edit
    ),
    DealItem(
        id = "gosuslugi",
        title = "Госуслуги",
        description = "Портал госуслуг РФ",
        type = DealType.WEB,
        url = "https://www.gosuslugi.ru",
        accentColor = NeonBlue,
        iconVector = Icons.Filled.AccountBalance
    ),
    DealItem(
        id = "ozon",
        title = "Ozon",
        description = "Маркетплейс",
        type = DealType.WEB,
        url = "https://www.ozon.ru",
        accentColor = NeonPurple,
        iconVector = Icons.Filled.ShoppingBag
    ),
    DealItem(
        id = "wb",
        title = "Wildberries",
        description = "Маркетплейс",
        type = DealType.WEB,
        url = "https://www.wildberries.ru",
        accentColor = NeonRed,
        iconVector = Icons.Filled.ShoppingCart
    ),
    DealItem(
        id = "drom",
        title = "Drom.ru",
        description = "Автомобили",
        type = DealType.WEB,
        url = "https://www.drom.ru",
        accentColor = NeonLime,
        iconVector = Icons.Filled.DirectionsCar
    ),
    DealItem(
        id = "avito",
        title = "Авито",
        description = "Доска объявлений",
        type = DealType.WEB,
        url = "https://www.avito.ru/",
        accentColor = NeonTeal,
        iconVector = Icons.Filled.Storefront
    ),
    DealItem(
        id = "RT",
        title = "Russia Today",
        description = "Новости",
        type = DealType.WEB,
        url = "https://russian.rt.com/",
        accentColor = NeonOrange,
        iconVector = Icons.Filled.Newspaper
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealsScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Дела",
                        fontWeight = FontWeight.Black,
                        color = NeonMagenta,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(dealItems) { item ->
                DealNeonItem(item = item, navController = navController)
            }
        }
    }
}

@Composable
fun DealNeonItem(item: DealItem, navController: NavHostController) {
    val context = LocalContext.current
    val glowColor = item.accentColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(12.dp, spotColor = glowColor, ambientColor = glowColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, glowColor.copy(alpha = 0.7f)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D0D0D)
        ),
        onClick = {
            when (item.type) {
                DealType.ACTIVITY -> {
                    when (item.id) {
                        "browser" -> context.startActivity(Intent(context, BrowserActivity::class.java))
                        "transfer" -> context.startActivity(Intent(context, TransferActivity::class.java))
                    }
                }
                DealType.CALCULATOR -> navController.navigate(Routes.CALCULATOR)
                DealType.TOOL -> {
                    when (item.id) {
                        "text_editor" -> navController.navigate(Routes.TEXT_EDITOR)
                        "file_manager" -> navController.navigate(Routes.FILE_MANAGER)
                    }
                }
                DealType.WEB -> {
                    item.url?.let {
                        val encodedUrl = URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
                        val encodedTitle = URLEncoder.encode(item.title, StandardCharsets.UTF_8.toString())
                        navController.navigate("webview?url=$encodedUrl&title=$encodedTitle")
                    }
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            glowColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Неоновая иконка с подложкой
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        glowColor.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, glowColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.iconVector ?: Icons.Filled.ShoppingBag,
                    contentDescription = null,
                    tint = glowColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.description,
                    color = glowColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    letterSpacing = 0.3.sp
                )
            }

            // Неоновая стрелка
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = glowColor.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
