package com.example.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.StartActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Chat

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherScreen(
                        onLaunchDeltaChat = { launchDeltaChat() },
                        onLaunchTyr = { launchTyr() }
                    )
                }
            }
        }
    }

    private fun launchDeltaChat() {
        val packageName = "com.b44t.messenger"
        
        if (isAppInstalled(packageName)) {
            // Приложение установлено - запускаем
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
                showToast("DeltaChat запущен")
            } else {
                showToast("Не удалось запустить DeltaChat")
            }
        } else {
            // Приложение не установлено - открываем в Google Play или скачиваем APK
            showToast("DeltaChat не установлен. Открываем страницу загрузки...")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                startActivity(intent)
            } catch (e: Exception) {
                // Если Google Play не доступен, открываем веб-версию
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://delta.chat/ru/download"))
                startActivity(intent)
            }
        }
    }

    private fun launchTyr() {
        val packageName = "com.bormental.tyr"
        
        if (isAppInstalled(packageName)) {
            // Приложение установлено - запускаем
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
                showToast("Tyr запущен")
            } else {
                showToast("Не удалось запустить Tyr")
            }
        } else {
            // Приложение не установлено - пробуем запустить через кастомную схему или APK
            showToast("Tyr не установлен. Попытка альтернативного запуска...")
            try {
                // Пробуем запустить через deep link
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tyr://launch"))
                startActivity(intent)
            } catch (e: Exception) {
                showToast("Не удалось запустить Tyr. Приложение не найдено.")
            }
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun LauncherScreen(
    onLaunchDeltaChat: () -> Unit,
    onLaunchTyr: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🚀 Запуск приложений",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Выберите приложение для запуска",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Кнопка DeltaChat
        Button(
            onClick = onLaunchDeltaChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF43E97B)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "DeltaChat",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Защищённая почта",
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Кнопка Tyr
        Button(
            onClick = onLaunchTyr,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B6B)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Tyr",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Почтовый клиент",
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Информация о статусе
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📱 Информация:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Если приложение установлено - оно будет запущено",
                    fontSize = 14.sp
                )
                Text(
                    text = "• Если не установлено - откроется страница загрузки",
                    fontSize = 14.sp
                )
            }
        }
    }
}
