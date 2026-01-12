package com.ikorihn.obsiditter.model

data class CategoryField(
    val key: String,
    val displayName: String,
    val type: FieldType = FieldType.ShortText
) {
    enum class FieldType {
        ShortText, // Single line text
        LongText,  // Multi-line text
        Select,    // Dropdown/Autocomplete from existing values
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
