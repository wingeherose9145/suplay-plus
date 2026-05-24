package com.nichudict

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream
import java.io.InputStream

// 加上 private val 确保 context 在成员方法中可用
class DictionaryDatabase(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "nichudict.db"
        private const val DATABASE_VERSION = 1
    }

    private val dbPath = context.getDatabasePath(DATABASE_NAME).absolutePath

    fun openDatabase(): SQLiteDatabase {
        // 如果数据库不存在，从 assets 复制
        if (!java.io.File(dbPath).exists()) {
            // 修正路径为根目录，并明确声明 input 和 output 的类型以解决"Cannot infer type"编译错误
            context.assets.open(DATABASE_NAME).use { input: InputStream ->
                FileOutputStream(dbPath).use { output: FileOutputStream ->
                    input.copyTo(output)
                }
            }
        }
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
    }

    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}
