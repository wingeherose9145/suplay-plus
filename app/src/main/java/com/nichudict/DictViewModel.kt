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

    // 使用最精准的明文表转换平假名到片假名
    private fun toKatakana(text: String): String {

    val hira =
        "ぁあぃいぅうぇえぉおゔ" +
        "かがきぎくぐけげこご" +
        "さざしじすずせぜそぞ" +
        "ただちぢっつづてでとど" +
        "なにぬねの" +
        "はばぱひびぴふぶぷへべぺほぼぽ" +
        "まみむめも" +
        "ゃやゅゆょよ" +
        "らりるれろ" +
        "ゎわをん" +
        "ゕゖ"

    val kata =
        "ァアィイゥウェエォオヴ" +
        "カガキギクグケゲコゴ" +
        "サザシジスズセゼソゾ" +
        "タダチヂッツヅテデトド" +
        "ナニヌネノ" +
        "ハバパヒビピフブプヘベペホボポ" +
        "マミムメモ" +
        "ャヤュユョヨ" +
        "ラリルレロ" +
        "ヮワヲン" +
        "ヵヶ"

    return text.map { char ->
        val idx = hira.indexOf(char)
        if (idx != -1) kata[idx] else char
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
                
                // 彻底锁定：完全匹配 > 声调序号智能过滤 > 普通前缀
                val sql = """
                    SELECT word, reading, html 
                    FROM dict 
                    WHERE word = ? 
                       OR word = ?
                       OR word LIKE ? 
                       OR word LIKE ?
                    ORDER BY 
                        (CASE 
                            WHEN word = ? OR word = ? THEN 1
                            WHEN word LIKE ? AND LENGTH(word) <= LENGTH(?) + 4 THEN 2
                            WHEN word LIKE ? AND LENGTH(word) <= LENGTH(?) + 4 THEN 3
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
