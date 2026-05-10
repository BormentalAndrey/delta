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
            val app = context?.applicationContext ?: return true
            
            // Проверяем, не инициализирован ли уже
            var alreadyInitialized = false
            try {
                alreadyInitialized = TyrApplication.Companion::instance.isInitialized
            } catch (_: Exception) {}
            if (alreadyInitialized) return true

            val tyrApp = TyrApplication()
            
            // Рефлексия для обхода private set в companion object
            val companionClass = Class.forName("com.jbselfcompany.tyr.TyrApplication\$Companion")
            val instanceField: Field = companionClass.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(TyrApplication.Companion, tyrApp)
            
            // Рефлексия для configRepository
            val configField: Field = TyrApplication::class.java.getDeclaredField("configRepository")
            configField.isAccessible = true
            configField.set(tyrApp, null) // будет установлен в onCreate
            
            tyrApp.onCreate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    override fun query(uri: Uri, p: Array<out String>?, sel: String?, sa: Array<out String>?, sort: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, sel: String?, sa: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, sel: String?, sa: Array<out String>?): Int = 0
}
