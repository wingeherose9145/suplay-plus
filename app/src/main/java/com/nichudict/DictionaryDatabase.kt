package com.nichudict

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DictionaryDatabase(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "nichudict.db"
        private const val DATABASE_VERSION = 1
    }

    private val dbPath = context.getDatabasePath(DATABASE_NAME).absolutePath

    fun openDatabase(): SQLiteDatabase {
        val dbFile = File(dbPath)
        
        // 如果数据库不存在，从 assets 复制
        if (!dbFile.exists()) {
            // 【关键修复】第一次安装时 databases 文件夹是不存在的，必须先创建它！
            dbFile.parentFile?.mkdirs()
            
            context.assets.open(DATABASE_NAME).use { input: InputStream ->
                FileOutputStream(dbFile).use { output: FileOutputStream ->
                    input.copyTo(output)
                }
            }
        }
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
    }

    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}
