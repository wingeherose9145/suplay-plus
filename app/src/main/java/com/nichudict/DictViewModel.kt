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

    // 辅助函数：将平假名转换为片假名
    private fun toKatakana(text: String): String {
        return text.map { char ->
            if (char in 'ぁ'..'ん') char + ('ァ' - 'ぁ') else char
        }.joinToString("")
    }

    fun search(query: String) {
        // 1. 清除两端空格
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
                
                // 2. 准备片假名副本，实现假名协同搜索
                val katakanaQuery = toKatakana(trimmedQuery)
                
                // 3. 终极精准权重 SQL 算法
                val sql = """
                    SELECT word, reading, html 
                    FROM dict 
                    WHERE word = ? 
                       OR word = ?
                       OR word LIKE ? 
                       OR word LIKE ?
                       OR reading LIKE ?
                    ORDER BY 
                        (CASE 
                            -- 1. 完全相等的词条（最高权重）
                            WHEN word = ? OR word = ? THEN 1
                            
                            -- 2. 带声调或汉字后缀的精准词条 (例如输入 うえ 匹配 うえ① 或 うえ【上】)
                            WHEN word LIKE ? AND LENGTH(word) <= LENGTH(?) + 4 THEN 2
                            WHEN word LIKE ? AND LENGTH(word) <= LENGTH(?) + 4 THEN 3
                            
                            -- 3. 普通前缀匹配 (例如输入 うえ 匹配 うえの)
                            WHEN word LIKE ? THEN 4
                            WHEN word LIKE ? THEN 5
                            
                            -- 4. 读音/包含匹配（最低权重）
                            ELSE 6
                        END), 
                        LENGTH(word) ASC, -- 越短越精准，防止长词霸屏
                        word ASC
                    LIMIT 40
                """.trimIndent()
                
                // 4. 严格对应 SQL 中的 ? 号绑定参数
                val args = arrayOf(
                    trimmedQuery,      // word = ? (平假名完全匹配)
                    katakanaQuery,     // word = ? (片假名完全匹配)
                    "$trimmedQuery%",  // word LIKE ? (平假名前缀)
                    "$katakanaQuery%", // word LIKE ? (片假名前缀)
                    "%$trimmedQuery%", // reading LIKE ? (读音包含)
                    
                    trimmedQuery,      // WHEN word = ?
                    katakanaQuery,     // OR word = ?
                    "$trimmedQuery%",  // WHEN word LIKE ? (平假名智能前缀)
                    trimmedQuery,      // LENGTH(?)
                    "$katakanaQuery%", // WHEN word LIKE ? (片假名智能前缀)
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
