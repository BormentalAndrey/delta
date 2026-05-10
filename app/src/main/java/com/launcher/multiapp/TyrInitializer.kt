package com.launcher.multiapp

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

class TyrInitializer : ContentProvider() {

    override fun onCreate(): Boolean {
        try {
            val app = context?.applicationContext as? android.app.Application ?: return true
            TyrInitHelper.init(app)
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
