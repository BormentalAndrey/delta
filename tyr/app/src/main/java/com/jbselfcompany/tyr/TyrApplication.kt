package com.launcher.multiapp

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.jbselfcompany.tyr.TyrApplication
import java.lang.reflect.Field

class TyrInitializer : ContentProvider() {

    override fun onCreate(): Boolean {
        try {
            val app = context?.applicationContext as? android.app.Application ?: return true
            
            // Проверяем, не инициализирован ли уже
            try {
                if (TyrApplication.Companion::instance.isInitialized) return true
            } catch (e: Exception) {
                // Не инициализирован — продолжаем
            }

            val tyrApp = TyrApplication()
            
            // Взламываем private set для instance
            val companionClass = TyrApplication.Companion::class.java
            val instanceField: Field = companionClass.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(TyrApplication.Companion, tyrApp)
            
            // Взламываем private set для configRepository
            val configRepo = com.jbselfcompany.tyr.data.ConfigRepository(app)
            val configField: Field = TyrApplication::class.java.getDeclaredField("configRepository")
            configField.isAccessible = true
            configField.set(tyrApp, configRepo)
            
            // Вызываем onCreate
            tyrApp.onCreate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
