package com.launcher.multiapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { startDeltaChat() },
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Запустить DeltaChat")
                        }
                    }
                }
            }
        }
    }

    private fun startDeltaChat() {
        try {
            // RoutingActivity — это главная точка входа в DeltaChat
            val intent = Intent().setClassName(
                this, 
                "org.thoughtcrime.securesms.RoutingActivity"
            )
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка запуска: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
