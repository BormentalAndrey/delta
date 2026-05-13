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
        super.attachBaseContext(LocaleHelper.applyLanguage(base))
    }

    override fun onCreate() {
        instance = this
        
        // 1. Инициализируем DeltaChat
        super.onCreate()
        
        // 2. Инициализируем Kakdela P2P (PDFBox)
        initKakdela()
        
        // 3. Инициализируем Tyr
        TyrApplication.init(this)
        
        // 4. Копируем ссылки
        configRepository = TyrApplication.instance.configRepository
    }

    private fun initKakdela() {
        try {
            com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(this)
            android.util.Log.i("TyrAppWrapper", "PDFBox initialized")
        } catch (e: Exception) {
            android.util.Log.e("TyrAppWrapper", "Kakdela init error", e)
        }
    }

    override fun onTerminate() {
        TyrApplication.instance.onTerminate()
        super.onTerminate()
    }
}
