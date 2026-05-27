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

        // 使用协程在后台线程执行数据库操作
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = dbHelper.openDatabase()
                
                // 【逻辑优化】：SQL 排序逻辑
                // 1. 完全匹配结果 (Rank 1)
                // 2. 以搜索词开头的结果 (Rank 2)
                // 3. 包含搜索词的结果 (Rank 3)
                val sql = """
                    SELECT word, content 
                    FROM entries 
                    WHERE word LIKE ? OR content LIKE ? 
                    ORDER BY 
                        (CASE 
                            WHEN word = ? THEN 1 
                            WHEN word LIKE ? THEN 2 
                            ELSE 3 
                        END), 
                        word ASC
                    LIMIT 50
                """.trimIndent()
                
                // 参数对应：Like模糊匹配1，Like模糊匹配2，完全匹配，开头匹配
                val args = arrayOf("%$query%", "%$query%", query, "$query%")
                val cursor = db.rawQuery(sql, args)

                val tempResults = mutableListOf<DictionaryEntry>()
                while (cursor.moveToNext()) {
                    tempResults.add(
                        DictionaryEntry(
                            word = cursor.getString(0) ?: "",
                            reading = "", 
                            content = cursor.getString(1) ?: "" // 使用 content 字段
                        )
                    )
                }
                cursor.close()
                db.close()
                
                // 回到主线程更新 UI
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
