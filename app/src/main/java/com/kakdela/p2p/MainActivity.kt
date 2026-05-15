package com.kakdela.p2p

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(packageName, "com.launcher.multiapp.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            finish()
            overridePendingTransition(0, 0)
        }
    }
}
