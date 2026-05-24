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
                val cursor = db.rawQuery(
                    """
                    SELECT word, reading, meaning 
                    FROM dictionary 
                    WHERE word LIKE ? OR reading LIKE ? OR meaning LIKE ?
                    LIMIT 50
                    """.trimIndent(),
                    arrayOf("%$query%", "%$query%", "%$query%")
                )

                val tempResults = mutableListOf<DictionaryEntry>()
                while (cursor.moveToNext()) {
                    tempResults.add(
                        DictionaryEntry(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2)
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
