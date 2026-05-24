package com.nichudict

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream

class DictionaryDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "nichudict.db"
        private const val DATABASE_VERSION = 1
    }

    private val dbPath = context.getDatabasePath(DATABASE_NAME).absolutePath

    fun openDatabase(): SQLiteDatabase {
        // 如果数据库不存在，从 assets 复制
        if (!java.io.File(dbPath).exists()) {
            context.assets.open("databases/$DATABASE_NAME").use { input ->
                FileOutputStream(dbPath).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
    }

    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}
