package com.nichudict

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class DictViewModel : ViewModel() {
    val searchText = mutableStateOf("")
    val results = mutableStateListOf<DictionaryEntry>()
    val isLoading = mutableStateOf(false)

    // 示例数据（你可以后续替换为真实大词典）
    private val dictionary = listOf(
        DictionaryEntry("食べる", "たべる", "吃；食用"),
        DictionaryEntry("学校", "がっこう", "学校"),
        DictionaryEntry("美しい", "うつくしい", "美丽的；美好的"),
        DictionaryEntry("こんにちは", "こんにちは", "你好"),
        DictionaryEntry("ありがとう", "ありがとう", "谢谢"),
        DictionaryEntry("日本", "にほん", "日本"),
        DictionaryEntry("勉強", "べんきょう", "学习；用功"),
        DictionaryEntry("友達", "ともだち", "朋友"),
    )

    fun search(query: String) {
        searchText.value = query
        if (query.isBlank()) {
            results.clear()
            return
        }

        isLoading.value = true
        results.clear()

        val lowerQuery = query.trim().lowercase()
        results.addAll(
            dictionary.filter {
                it.word.contains(lowerQuery, ignoreCase = true) ||
                it.reading.contains(lowerQuery, ignoreCase = true) ||
                it.meaning.contains(lowerQuery, ignoreCase = true)
            }
        )

        isLoading.value = false
    }
}
