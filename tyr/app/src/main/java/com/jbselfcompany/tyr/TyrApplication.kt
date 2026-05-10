package com.jbselfcompany.tyr

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.jbselfcompany.tyr.data.ConfigRepository
import com.jbselfcompany.tyr.receiver.NetworkChangeReceiver
import com.jbselfcompany.tyr.utils.LocaleHelper
import com.jbselfcompany.tyr.utils.TyrLogger

// 1. Наследуемся от ContextWrapper. Теперь класс работает как полноценный Context
class TyrApplication private constructor(private val app: Application) : ContextWrapper(app) {

    companion object {
        const val CHANNEL_ID_SERVICE = "yggmail_service"
        const val CHANNEL_ID_MAIL = "mail_notifications"
        const val CHANNEL_ID_CHAT = "chat_notifications"

        lateinit var instance: TyrApplication
            private set

        // 2. Специальный метод, который вызовет лаунчер
        fun init(application: Application) {
            instance = TyrApplication(application)
            instance.initialize()
        }
    }

    lateinit var configRepository: ConfigRepository
        private set

    var yggmailServiceBinder: com.jbselfcompany.tyr.service.YggmailService.LocalBinder? = null

    private var networkCallback: NetworkChangeReceiver? = null
    private val networkCallbackHandler = Handler(Looper.getMainLooper())
    private var networkCallbackRunnable: Runnable? = null

    // 3. Бывший onCreate() стал обычной функцией
    private fun initialize() {
        configRepository = ConfigRepository(this)
        TyrLogger.setEnabled(configRepository.isLogCollectionEnabled())
        LocaleHelper.applyTheme(this)
        createNotificationChannels()

        if (configRepository.isServiceEnabled() && configRepository.isAutoStartEnabled()) {
            scheduleNetworkCallbackRegistration()
        }
    }

    private fun scheduleNetworkCallbackRegistration() {
        networkCallbackRunnable = Runnable {
            networkCallback = NetworkChangeReceiver(this)
            networkCallback?.register()
        }
        networkCallbackHandler.postDelayed(networkCallbackRunnable!!, 15000)
    }

    fun cancelNetworkCallbackRegistration() {
        networkCallbackRunnable?.let {
            networkCallbackHandler.removeCallbacks(it)
        }
    }

    fun onTerminate() {
        cancelNetworkCallbackRegistration()
        networkCallback?.unregister()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.notification_channel_service_desc)
                setShowBadge(false)
            }

            val mailChannel = NotificationChannel(
                CHANNEL_ID_MAIL,
                getString(R.string.notification_channel_mail),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_mail_desc)
                setShowBadge(true)
            }

            val chatChannel = NotificationChannel(
                CHANNEL_ID_CHAT,
                getString(R.string.notification_channel_chat),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_chat_desc)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(mailChannel)
            notificationManager.createNotificationChannel(chatChannel)
        }
    }
}
