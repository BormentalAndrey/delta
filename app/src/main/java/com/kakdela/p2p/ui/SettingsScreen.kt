package com.kakdela.p2p.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.jbselfcompany.tyr.service.YggmailService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController? = null) {
    val context = LocalContext.current
    var isRestarting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Настройки", color = Color.Cyan, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Статус сервера
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Yggmail Сервер",
                        color = Color.Cyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (YggmailService.isRunning) "✅ Запущен" else "❌ Остановлен",
                        color = if (YggmailService.isRunning) Color(0xFF4CAF50) else Color(0xFFFF4444),
                        fontSize = 14.sp
                    )
                }
            }

            // Кнопка перезапуска (обновленный дизайн и логика)
            Button(
                onClick = {
                    isRestarting = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            // 1. Остановить сервер если запущен
                            if (YggmailService.isRunning) {
                                YggmailService.stop(context)
                                delay(1000)
                            }
                            
                            // 2. Запустить заново
                            YggmailService.start(context)
                            
                            // 3. Подождать готовности
                            var ready = false
                            repeat(30) {
                                delay(1000)
                                try {
                                    Socket().use { s ->
                                        s.connect(InetSocketAddress("127.0.0.1", 1143), 2000)
                                        ready = true
                                    }
                                } catch (_: Exception) {}
                                if (ready) return@repeat
                            }
                            
                            withContext(Dispatchers.Main) {
                                isRestarting = false
                                Toast.makeText(context, if (ready) "✅ Сервер перезапущен!" else "⚠️ Сервер запущен, но не отвечает", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isRestarting = false
                                Toast.makeText(context, "❌ Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = !isRestarting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FFFF).copy(alpha = 0.2f),
                    disabledContainerColor = Color(0xFF00FFFF).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isRestarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF00FFFF),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Перезапуск...", color = Color(0xFF00FFFF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                } else {
                    Text("🔄 Перезапустить сервер", color = Color(0xFF00FFFF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
