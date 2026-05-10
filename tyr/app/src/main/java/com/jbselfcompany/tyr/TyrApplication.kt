package com.launcher.multiapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.jbselfcompany.tyr.TyrApplication
import com.jbselfcompany.tyr.data.ConfigRepository
import com.jbselfcompany.tyr.receiver.NetworkChangeReceiver
import com.jbselfcompany.tyr.utils.LocaleHelper
import com.jbselfcompany.tyr.utils.TyrLogger

class TyrApplicationWrapper : com.b44t.messenger.ApplicationContext() {

    companion object {
        lateinit var instance: TyrApplicationWrapper
            private set
    }

    // Делегируем константы из TyrApplication
    val CHANNEL_ID_SERVICE = TyrApplication.CHANNEL_ID_SERVICE
    val CHANNEL_ID_MAIL = TyrApplication.CHANNEL_ID_MAIL
    val CHANNEL_ID_CHAT = TyrApplication.CHANNEL_ID_CHAT

    lateinit var configRepository: ConfigRepository
        private set

    var yggmailServiceBinder: com.jbselfcompany.tyr.service.YggmailService.LocalBinder? = null

    // Ссылка на TyrApplication для вызова initializeApplication()
    private lateinit var tyrApp: TyrApplication

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Создаём экземпляр TyrApplication и вызываем его инициализацию
        tyrApp = TyrApplication()
        tyrApp.attachBaseContext(this)
        tyrApp.initializeApplication()
        
        // Копируем свойства из TyrApplication
        configRepository = tyrApp.configRepository
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(base))
    }

    override fun onTerminate() {
        tyrApp.onTerminate()
        super.onTerminate()
    }
}
