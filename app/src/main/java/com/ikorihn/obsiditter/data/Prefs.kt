package com.ikorihn.obsiditter.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit

class Prefs(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences("obsiditter_prefs", Context.MODE_PRIVATE)

    var storageUri: Uri?
        get() {
            val uriString = sharedPreferences.getString("storage_uri", null)
            return uriString?.let { Uri.parse(it) }
        }
        set(value) {
            sharedPreferences.edit {
                putString("storage_uri", value?.toString())
            }
        }
}
