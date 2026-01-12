package com.ikorihn.obsiditter.model

data class CategoryField(
    val key: String,
    val displayName: String,
    val type: FieldType = FieldType.String
) {
    enum class FieldType {
        String,
        Date,
        List,
    }
}

data class Category(
    val id: String, // Unique ID (e.g. UUID)
    val displayName: String,
    val folderName: String,
    val fields: List<CategoryField> = emptyList()
)
