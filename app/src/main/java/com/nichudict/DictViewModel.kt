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
                
                // 修改为适配小词库 (dict 表)
                val sql = """
                    SELECT word, reading, html 
                    FROM dict 
                    WHERE word LIKE ? OR html LIKE ? 
                    ORDER BY 
                        (CASE 
                            WHEN word = ? THEN 1 
                            WHEN word LIKE ? THEN 2 
                            ELSE 3 
                        END), 
                        word ASC
                    LIMIT 50
                """.trimIndent()
                
                val args = arrayOf("%$query%", "%$query%", query, "$query%")
                val cursor = db.rawQuery(sql, args)

                val tempResults = mutableListOf<DictionaryEntry>()
                while (cursor.moveToNext()) {
                    tempResults.add(
                        DictionaryEntry(
                            word = cursor.getString(0) ?: "",
                            reading = cursor.getString(1) ?: "",   // 使用 reading 字段
                            content = cursor.getString(2) ?: ""    // 使用 html 字段
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
