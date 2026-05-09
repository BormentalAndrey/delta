package com.example.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send // Используем Send вместо Chat, если библиотека расширенных иконок не подключена
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                // ИСПРАВЛЕНО: функция называется startActivity
                startActivity(launchIntent)
                showToast("DeltaChat запущен")
            } else {
                showToast("Не удалось запустить DeltaChat")
            }
        } else {
            showToast("DeltaChat не установлен. Открываем страницу загрузки...")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://delta.chat/ru/download"))
                startActivity(intent)
            }
        }
    }

    private fun launchTyr() {
        // Убедитесь, что ID совпадает с вашим проектом (com.jbselfcompany.tyr или com.bormental.tyr)
        val packageName = "com.bormental.tyr"
        
        if (isAppInstalled(packageName)) {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
                showToast("Tyr запущен")
            } else {
                showToast("Не удалось запустить Tyr")
            }
        } else {
            showToast("Tyr не установлен. Попытка альтернативного запуска...")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tyr://launch"))
                startActivity(intent)
            } catch (e: Exception) {
                showToast("Приложение Tyr не найдено.")
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
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onLaunchDeltaChat,
            modifier = Modifier.fillMaxWidth().height(80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43E97B))
        ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "DeltaChat", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onLaunchTyr,
            modifier = Modifier.fillMaxWidth().height(80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
        ) {
            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Tyr", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
