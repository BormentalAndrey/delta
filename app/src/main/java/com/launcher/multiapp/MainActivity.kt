package com.launcher.multiapp

import android.content.Intent
import android.os.Bundle
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onLaunchDeltaChat = { launchDeltaChat() },
                        onLaunchTyr = { launchTyr() }
                    )
                }
            }
        }
    }

    private fun launchDeltaChat() {
        try {
            // Теперь DeltaChat встроен — запускаем его основную Activity напрямую
            val intent = Intent(this, org.thoughtcrime.securesms.RoutingActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchTyr() {
        try {
            val intent = Intent(this, com.jbselfcompany.tyr.ui.MainActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun MainScreen(
    onLaunchDeltaChat: () -> Unit,
    onLaunchTyr: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🚀 Universal Launcher",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Выберите приложение для запуска",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onLaunchDeltaChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = MaterialTheme.shapes.large
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💬", fontSize = 28.sp)
                Text("DeltaChat", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Защищённый мессенджер", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLaunchTyr,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
            shape = MaterialTheme.shapes.large
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📧", fontSize = 28.sp)
                Text("Tyr", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Почтовый клиент", fontSize = 13.sp)
            }
        }
    }
}
