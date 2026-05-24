package com.nichudict

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

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

        while (cursor.moveToNext()) {
            results.add(
                DictionaryEntry(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2)
                )
            )
        }
        cursor.close()
        db.close()
        isLoading.value = false
    }
}
