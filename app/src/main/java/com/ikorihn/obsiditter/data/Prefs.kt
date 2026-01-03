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

    var noteTemplate: String
        get() = sharedPreferences.getString("note_template", defaultTemplate) ?: defaultTemplate
        set(value) {
            sharedPreferences.edit {
                putString("note_template", value)
            }
        }

    companion object {
        const val defaultTemplate = """---
date: "{{date}}"
tags:
- "daily"
fileClass: "DailyLog"
sleep_time: null
wake_time: null
morning: []
lunch: []
dinner: []
snacks: []
reading_min: 0
exercise_min: 0
---

## Memo

## Journal
"""
    }
}
