package com.launcher.multiapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.jbselfcompany.tyr.data.ConfigRepository
import com.jbselfcompany.tyr.receiver.NetworkChangeReceiver
import com.jbselfcompany.tyr.utils.LocaleHelper
import com.jbselfcompany.tyr.utils.TyrLogger

class TyrApplicationWrapper : com.b44t.messenger.ApplicationContext() {

    companion object {
        const val CHANNEL_ID_SERVICE = "yggmail_service"
        const val CHANNEL_ID_MAIL = "mail_notifications"
        const val CHANNEL_ID_CHAT = "chat_notifications"

        lateinit var instance: TyrApplicationWrapper
            private set
    }

    lateinit var configRepository: ConfigRepository
        private set

    var yggmailServiceBinder: com.jbselfcompany.tyr.service.YggmailService.LocalBinder? = null

    private var networkCallback: NetworkChangeReceiver? = null
    private val networkCallbackHandler = Handler(Looper.getMainLooper())
    private var networkCallbackRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
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

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(base))
    }

    override fun onTerminate() {
        cancelNetworkCallbackRegistration()
        networkCallback?.unregister()
        super.onTerminate()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Yggmail Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Yggmail background service"
                setShowBadge(false)
            }

            val mailChannel = NotificationChannel(
                CHANNEL_ID_MAIL,
                "Mail Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New mail notifications"
                setShowBadge(true)
            }

            val chatChannel = NotificationChannel(
                CHANNEL_ID_CHAT,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat message notifications"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(mailChannel)
            notificationManager.createNotificationChannel(chatChannel)
        }
    }
}
