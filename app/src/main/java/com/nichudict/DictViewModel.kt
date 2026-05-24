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

        // 使用协程在后台线程执行数据库操作，防止界面卡死
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = dbHelper.openDatabase()
                
                // 【核心修改】：SQL 查询语句已适配 entries 表和 content 列
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
                    // getString(0) 是 word，getString(1) 是 content
                    tempResults.add(
                        DictionaryEntry(
                            word = cursor.getString(0) ?: "",
                            reading = "", // 数据库中没有读音列，留空
                            meaning = cursor.getString(1) ?: ""
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
                    // 报错时显示信息，方便排查
                    results.add(
                        DictionaryEntry(
                            word = "查询失败",
                            reading = "",
                            meaning = "错误原因: ${e.localizedMessage}"
                        )
                    )
                }
            }
        }
    }
}
