package com.nichudict

data class DictionaryEntry(
    val word: String,
    val reading: String, // 如果数据库里没这个字段，也可以保留，或者将其置为空
    val content: String  // 关键：这里直接用 content
)
