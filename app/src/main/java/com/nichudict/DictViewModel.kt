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
                
                // 执行查询，查询 word 或 content
                val cursor = db.rawQuery(
                    """
                    SELECT word, content 
                    FROM entries 
                    WHERE word LIKE ? OR content LIKE ? 
                    LIMIT 50
                    """.trimIndent(),
                    arrayOf("%$query%", "%$query%")
                )

                val tempResults = mutableListOf<DictionaryEntry>()
                while (cursor.moveToNext()) {
                    tempResults.add(
                        DictionaryEntry(
                            word = cursor.getString(0) ?: "",
                            reading = "", 
                            content = cursor.getString(1) ?: "" // 【关键修改】：这里现在是 content
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
                    // 报错时显示信息
                    results.add(
                        DictionaryEntry(
                            word = "查询失败",
                            reading = "",
                            content = "错误原因: ${e.localizedMessage ?: "未知错误"}" // 【关键修改】：这里也必须是 content
                        )
                    )
                }
            }
        }
    }
}
