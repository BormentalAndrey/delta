package com.launcher.multiapp

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.jbselfcompany.tyr.TyrApplication
import java.lang.reflect.Field

/**
 * ContentProvider that initializes TyrApplication before any Activity starts.
 * Uses reflection to bypass private setters on instance and configRepository.
 */
class TyrInitializer : ContentProvider() {

    override fun onCreate(): Boolean {
        try {
            val app = context?.applicationContext as? android.app.Application ?: return true

            if (isTyrInitialized()) return true

            val tyrApp = TyrApplication()
            val configRepo = com.jbselfcompany.tyr.data.ConfigRepository(app)

            val companionClass = TyrApplication.Companion::class.java
            val instanceField: Field = companionClass.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(TyrApplication.Companion, tyrApp)

            val configField: Field = TyrApplication::class.java.getDeclaredField("configRepository")
            configField.isAccessible = true
            configField.set(tyrApp, configRepo)

            tyrApp.onCreate()

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    private fun isTyrInitialized(): Boolean {
        return try {
            TyrApplication.Companion::instance.isInitialized
        } catch (e: Exception) {
            false
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
