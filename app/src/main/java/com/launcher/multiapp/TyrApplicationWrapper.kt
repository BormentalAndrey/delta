package com.launcher.multiapp

import android.content.Context
import com.jbselfcompany.tyr.TyrApplication
import com.jbselfcompany.tyr.data.ConfigRepository
import com.jbselfcompany.tyr.utils.LocaleHelper

class TyrApplicationWrapper : org.thoughtcrime.securesms.ApplicationContext() {

    companion object {
        lateinit var instance: TyrApplicationWrapper
            private set
    }

    lateinit var configRepository: ConfigRepository
        private set

    var yggmailServiceBinder: com.jbselfcompany.tyr.service.YggmailService.LocalBinder? = null

    private lateinit var tyrApp: TyrApplication

    override fun onCreate() {
        instance = this
        
        // Сначала инициализируем DeltaChat
        super.onCreate()
        
        // Создаём и инициализируем Tyr
        tyrApp = TyrApplication()
        tyrApp.attachBaseContext(this)
        tyrApp.onCreate()
        
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
