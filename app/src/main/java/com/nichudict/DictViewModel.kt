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
                
                // 【优化 1】：改进 SQL 搜索逻辑
                // 让“单词本身”包含搜索词的结果排在最前面，解释里包含的排在后面
                // 并且字数越短的排越前（越短通常越是精准匹配的基础词汇）
                val cursor = db.rawQuery(
                    """
                    SELECT word, html 
                    FROM entries 
                    WHERE word LIKE ? OR html LIKE ?
                    ORDER BY 
                        (word LIKE ?) DESC, 
                        LENGTH(word) ASC
                    LIMIT 50
                    """.trimIndent(),
                    // 对应上面的三个 ? 占位符
                    arrayOf("%$query%", "%$query%", "$query%") 
                )

                val tempResults = mutableListOf<DictionaryEntry>()
                while (cursor.moveToNext()) {
                    val rawWord = cursor.getString(0) ?: ""
                    val rawHtml = cursor.getString(1) ?: ""

                    // 【优化 2】：清洗 HTML 乱码代码
                    // 1. 把网页里的换行符 <br> 转换成手机能识别的真实换行
                    val textWithNewlines = rawHtml.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                    // 2. 用正则表达式暴力剔除所有 < > 里面的英文标签和多余代码
                    val cleanMeaning = textWithNewlines.replace(Regex("<.*?>"), "").trim()

                    // 【优化 3】：美化日文单词的显示
                    // 你的截图里有些词带有 '|' 符号（比如 あかい|紅い|赤い），我们把它换成空格看起来更清爽
                    val cleanWord = rawWord.replace("|", "  ")

                    tempResults.add(
                        DictionaryEntry(
                            word = cleanWord,
                            reading = "", // 发音已经包含在 word 字段里了
                            meaning = cleanMeaning
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
                    results.add(
                        DictionaryEntry(
                            word = "程序报错啦！",
                            reading = "请看下方原因：",
                            meaning = e.toString()
                        )
                    )
                }
            }
        }
    }
}
