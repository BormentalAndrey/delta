package com.launcher.multiapp

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.jbselfcompany.tyr.TyrApplication

/**
 * ContentProvider that initializes TyrApplication before any Activity starts.
 * This is needed because the app uses DeltaChat's ApplicationContext,
 * so TyrApplication.onCreate() is never called automatically.
 */
class TyrInitializer : ContentProvider() {

    override fun onCreate(): Boolean {
        val app = context?.applicationContext as? android.app.Application
        if (app != null && !TyrApplication.Companion::instance.isInitialized) {
            try {
                // Initialize TyrApplication manually
                TyrApplication.instance = TyrApplication()
                TyrApplication.instance.configRepository = 
                    com.jbselfcompany.tyr.data.ConfigRepository(app)
                TyrApplication.instance.onCreate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return true
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
