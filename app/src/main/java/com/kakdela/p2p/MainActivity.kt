package com.kakdela.p2p

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kakdela.p2p.ui.onboarding.OnboardingScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем SharedPreferences для проверки статуса
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isAgreementAccepted = sharedPrefs.getBoolean("agreement_accepted", false)

        if (isAgreementAccepted) {
            // Если уже согласен — летим в главное приложение
            navigateToMainApp()
        } else {
            // Если запускает впервые — рендерим экран соглашения
            setContent {
                OnboardingScreen(
                    onFinished = {
                        // Сохраняем выбор пользователя, чтобы экран больше не появлялся
                        sharedPrefs.edit().putBoolean("agreement_accepted", true).apply()
                        // Переходим в главное приложение
                        navigateToMainApp()
                    }
                )
            }
        }
    }

    private fun navigateToMainApp() {
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
