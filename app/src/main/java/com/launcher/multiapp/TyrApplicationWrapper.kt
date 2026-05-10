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

    override fun attachBaseContext(base: Context) {
        // Локаль применяется глобально для всего объединенного приложения
        super.attachBaseContext(LocaleHelper.applyLanguage(base))
    }

    override fun onCreate() {
        instance = this
        
        // 1. Инициализируем DeltaChat
        super.onCreate()
        
        // 2. Безопасно инициализируем Tyr, передавая легальный Application Context
        TyrApplication.init(this)
        
        // 3. Копируем нужные ссылки для внешнего доступа
        configRepository = TyrApplication.instance.configRepository
    }

    override fun onTerminate() {
        TyrApplication.instance.onTerminate()
        super.onTerminate()
    }
}
