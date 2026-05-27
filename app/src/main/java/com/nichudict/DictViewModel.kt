package com.nichudict

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DictViewModel(application: Application) : AndroidViewModel(application) {
    val searchText = mutableStateOf("")
    val results = mutableStateListOf<DictionaryEntry>()
    val isLoading = mutableStateOf(false)

    private val dbHelper = DictionaryDatabase(application)

    // 将平假名高效转换为片假名，用于协同双检索
    private fun toKatakana(text: String): String {
        return text.map { char ->
            if (char in 'ぁ'..'ん') char + ('ァ' - 'ぁ') else char
        }.joinToString("")
    }

    fun search(query: String) {
        val trimmedQuery = query.trim()
        searchText.value = trimmedQuery
        
        if (trimmedQuery.isBlank()) {
            results.clear()
            return
        }

        isLoading.value = true
        results.clear()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = dbHelper.openDatabase()
                val katakanaQuery = toKatakana(trimmedQuery)
                
                // 彻底精简并收紧匹配规则，不允许大跨度模糊词霸屏
                val sql = """
                    SELECT word, reading, html 
                    FROM dict 
                    WHERE word = ? 
                       OR word = ?
                       OR word LIKE ? 
                       OR word LIKE ?
                    ORDER BY 
                        (CASE 
                            -- 1. 绝对相等（全字精准命中）
                            WHEN word = ? OR word = ? THEN 1
                            
                            -- 2. 携带声调/词性后缀的词条（小学馆特性：如输入 うえ 匹配 うえ① 或 うえ【上】）
                            WHEN word LIKE ? AND LENGTH(word) <= LENGTH(?) + 4 THEN 2
                            WHEN word LIKE ? AND LENGTH(word) <= LENGTH(?) + 4 THEN 3
                            
                            -- 3. 普通前缀匹配（如输入 うえ 匹配 うえの）
                            WHEN word LIKE ? THEN 4
                            WHEN word LIKE ? THEN 5
                            
                            ELSE 6
                        END), 
                        LENGTH(word) ASC, 
                        word ASC
                    LIMIT 30
                """.trimIndent()
                
                val args = arrayOf(
                    trimmedQuery,      // word = ?
                    katakanaQuery,     // word = ?
                    "$trimmedQuery%",  // word LIKE ?
                    "$katakanaQuery%", // word LIKE ?
                    
                    trimmedQuery,      // WHEN word = ?
                    katakanaQuery,     // OR word = ?
                    "$trimmedQuery%",  // WHEN word LIKE ?
                    trimmedQuery,      // LENGTH(?)
                    "$katakanaQuery%", // WHEN word LIKE ?
                    katakanaQuery,     // LENGTH(?)
                    "$trimmedQuery%",  // WHEN word LIKE ?
                    "$katakanaQuery%"  // WHEN word LIKE ?
                )
                
                val cursor = db.rawQuery(sql, args)

                val tempResults = mutableListOf<DictionaryEntry>()
                while (cursor.moveToNext()) {
                    tempResults.add(
                        DictionaryEntry(
                            word = cursor.getString(0) ?: "",
                            reading = cursor.getString(1) ?: "",   
                            content = cursor.getString(2) ?: ""    
                        )
                    )
                }
                cursor.close()
                db.close()
                
                withContext(Dispatchers.Main) {
                    results.addAll(tempResults)
                    isLoading.value = false
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isLoading.value = false
                    results.clear()
                }
            }
        }
    }
}
