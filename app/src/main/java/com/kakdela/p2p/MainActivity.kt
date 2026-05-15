package com.kakdela.p2p

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Инициализируем системный SplashScreen API.
        // Это должно быть ПЕРЕД super.onCreate(savedInstanceState)
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // 2. Логика перенаправления на основной лаунчер.
        // Мы используем явный Intent, чтобы гарантированно попасть в нужную Activity.
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(packageName, "com.launcher.multiapp.MainActivity")
            // FLAG_ACTIVITY_NO_ANIMATION поможет убрать лишние визуальные скачки при переходе
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // На случай, если пакет или класс не найдены (например, при рефакторинге)
            e.printStackTrace()
        } finally {
            // 3. Закрываем текущую Activity, чтобы она не висела в стеке
            finish()
            // Убираем анимацию закрытия, чтобы переход был бесшовным (черный на черный)
            overridePendingTransition(0, 0)
        }
    }
}
