package com.launcher.multiapp

import android.app.Application
import com.jbselfcompany.tyr.TyrApplication

/**
 * Universal Application class that initializes both DeltaChat and Tyr.
 * DeltaChat uses org.thoughtcrime.securesms.ApplicationContext,
 * Tyr uses com.jbselfcompany.tyr.TyrApplication.
 * 
 * This class delegates initialization to both.
 */
class UniversalApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Tyr first
        try {
            TyrApplication.instance = TyrApplication()
            TyrApplication.instance.configRepository = 
                com.jbselfcompany.tyr.data.ConfigRepository(this)
            TyrApplication.instance.onCreate()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // DeltaChat ApplicationContext handles itself via its own static init
        org.thoughtcrime.securesms.ApplicationContext.dcAccounts
    }
}
