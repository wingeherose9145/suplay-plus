package com.nichudict

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class DictViewModel : ViewModel() {
    val searchText = mutableStateOf("")
    val results = mutableStateListOf<DictionaryEntry>()

    // 示例词典数据（后续可替换为 MDX 加载）
    private val dictionary = listOf(
        DictionaryEntry("食べる", "たべる", "吃、食用"),
        DictionaryEntry("学校", "がっこう", "学校"),
        DictionaryEntry("美しい", "うつくしい", "美丽的"),
        DictionaryEntry("こんにちは", "こんにちは", "你好"),
        DictionaryEntry("ありがとう", "ありがとう", "谢谢"),
        DictionaryEntry("日本", "にほん", "日本"),
    )

    fun search(query: String) {
        searchText.value = query
        if (query.isBlank()) {
            results.clear()
            return
        }
        results.clear()
        val lowerQuery = query.lowercase()
        results.addAll(
            dictionary.filter {
                it.word.contains(lowerQuery) || 
                it.reading.contains(lowerQuery) || 
                it.meaning.contains(lowerQuery)
            }
        )
    }
}
