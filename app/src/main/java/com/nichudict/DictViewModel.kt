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

        // 【关键修复】将耗时的数据库拷贝和查询放到 IO 后台线程，防止卡死 UI 导致闪退
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = dbHelper.openDatabase()
                
                // 1. 修改表名为 entries，列名改为 word 和 html
                val cursor = db.rawQuery(
                    """
                    SELECT word, html 
                    FROM entries 
                    WHERE word LIKE ? OR html LIKE ?
                    LIMIT 50
                    """.trimIndent(),
                    arrayOf("%$query%", "%$query%") // 因为只查两列，所以这里变成两个参数
                )

                val tempResults = mutableListOf<DictionaryEntry>()
                while (cursor.moveToNext()) {
                    // 2. 将游标读取的数据装载到 DictionaryEntry 里
                    tempResults.add(
                        DictionaryEntry(
                            word = cursor.getString(0) ?: "",
                            reading = "", // 你的数据库没有独立发音字段了，这里先留空
                            meaning = cursor.getString(1) ?: "" // 将 html 列的内容作为解释
                        )
                    )
                }
                cursor.close()
                db.close()
                
                // 切换回主线程更新 UI
                withContext(Dispatchers.Main) {
                    results.addAll(tempResults)
                    isLoading.value = false
                }
                
                            
            } catch (e: Exception) {
                // 【神级调试技巧】拦截闪退！把错误直接显示在手机屏幕上
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isLoading.value = false
                    results.clear()
                    results.add(
                        DictionaryEntry(
                            word = "程序报错啦！",
                            reading = "请看下方原因：",
                            meaning = e.toString() // 比如这里会显示 "no such table: dictionary" 
                        )
                    )
                }
            }
        }
    }
}
