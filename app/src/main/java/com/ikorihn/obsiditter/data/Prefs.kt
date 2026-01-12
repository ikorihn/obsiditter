package com.ikorihn.obsiditter.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ikorihn.obsiditter.model.Category
import com.ikorihn.obsiditter.model.CategoryField

class Prefs(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences("obsiditter_prefs", Context.MODE_PRIVATE)
    private val objectMapper = ObjectMapper().registerKotlinModule()

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

    var categories: List<Category>
        get() {
            val json =
                sharedPreferences.getString("custom_categories", null) ?: return defaultCategories
            return try {
                objectMapper.readValue(json, object : TypeReference<List<Category>>() {})
            } catch (e: Exception) {
                defaultCategories
            }
        }
        set(value) {
            val json = objectMapper.writeValueAsString(value)
            sharedPreferences.edit {
                putString("custom_categories", json)
            }
        }

    fun getCategoryUri(category: Category): Uri? {
        val uriString = sharedPreferences.getString("category_uri_${category.id}", null)
        return uriString?.let { Uri.parse(it) }
    }

    fun setCategoryUri(category: Category, uri: Uri?) {
        sharedPreferences.edit {
            putString("category_uri_${category.id}", uri?.toString())
        }
    }

    companion object {
        val defaultCategories = listOf(
            Category(
                "movies",
                "Movies",
                "movies",
                listOf(
                    CategoryField("title", "Title"),
                    CategoryField("director", "Director"),
                    CategoryField("starring", "Starring"),
                    CategoryField("delivered_date", "Delivered", CategoryField.FieldType.Date),
                )
            ),
            Category(
                "books",
                "Reading",
                "books",
                listOf(
                    CategoryField("title", "Title"),
                    CategoryField("author", "Author"),
                    CategoryField("url", "URL"),
                    CategoryField("published_date", "Published", CategoryField.FieldType.Date),
                )
            ),
            Category(
                "comics",
                "Comics",
                "comics",
                listOf(
                    CategoryField("title", "Title"),
                    CategoryField("author", "Author"),
                )
            ),
            Category(
                "events",
                "Live",
                "events",
                listOf(
                    CategoryField("title", "Title"),
                    CategoryField("performers", "Performers", CategoryField.FieldType.List),
                    CategoryField("delivered_date", "Delivered", CategoryField.FieldType.Date),
                )
            ),
            Category(
                "videos",
                "Video Works",
                "videos",
                listOf(
                    CategoryField("title", "Title"),
                    CategoryField("tags", "Tags", CategoryField.FieldType.List),
                )
            ),
            Category(
                "podcasts",
                "Radio/Podcast/YouTube",
                "podcasts",
                listOf(
                    CategoryField("title", "Title", CategoryField.FieldType.ShortText),
                    CategoryField("program", "Program/Channel", CategoryField.FieldType.Select),
                    CategoryField("delivered_date", "Delivered", CategoryField.FieldType.Date),
                    CategoryField("media", "Media", CategoryField.FieldType.Select),
                    CategoryField("url", "URL", CategoryField.FieldType.ShortText),
                )
            ),
        )

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
exercise: []
reading_min: 0
exercise_min: 0
---

## Memo

## Journal
"""
    }
}