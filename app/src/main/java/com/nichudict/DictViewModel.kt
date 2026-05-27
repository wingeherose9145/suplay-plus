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

    fun search(query: String) {
        searchText.value = query
        if (query.isBlank()) {
            results.clear()
            return
        }

        isLoading.value = true
        results.clear()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = dbHelper.openDatabase()
                
                // 极致优化版 SQL：不再查询 html 字段，避免乱序；严格按匹配精准度排序
                val sql = """
                    SELECT word, reading, html 
                    FROM dict 
                    WHERE word = ? OR word LIKE ? OR word LIKE ?
                    ORDER BY 
                        (CASE 
                            WHEN word = ? THEN 1            -- 完全相等权重最高
                            WHEN word LIKE ? THEN 2         -- 前缀匹配次之 (例如输入 うえ 匹配 うえの)
                            ELSE 3                          -- 包含匹配权重最低
                        END), 
                        LENGTH(word) ASC,                   -- 词条越短越精准，防止长词干扰
                        word ASC
                    LIMIT 50
                """.trimIndent()
                
                // 参数依次对应 SQL 中的 ? 号
                val args = arrayOf(
                    query,          // word = ?
                    "$query%",      // word LIKE ? (前缀)
                    "%$query%",     // word LIKE ? (包含)
                    query,          // WHEN word = ?
                    "$query%"       // WHEN word LIKE ?
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
