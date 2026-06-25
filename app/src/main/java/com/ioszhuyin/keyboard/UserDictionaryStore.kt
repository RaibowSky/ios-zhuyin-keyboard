package com.ioszhuyin.keyboard

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class UserDictionaryEntry(
    val id: Long,
    val zhuyin: String,
    val word: String,
    val createdAt: Long,
    val updatedAt: Long
)

class UserDictionaryStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ZHUYIN TEXT NOT NULL,
                $COL_WORD TEXT NOT NULL,
                $COL_CREATED_AT INTEGER NOT NULL,
                $COL_UPDATED_AT INTEGER NOT NULL,
                UNIQUE($COL_ZHUYIN, $COL_WORD)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_user_dict_zhuyin ON $TABLE_NAME($COL_ZHUYIN)")
        db.execSQL("CREATE INDEX idx_user_dict_word ON $TABLE_NAME($COL_WORD)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_dict_zhuyin ON $TABLE_NAME($COL_ZHUYIN)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_dict_word ON $TABLE_NAME($COL_WORD)")
        }
    }

    fun addEntry(zhuyin: String, word: String): Long {
        val cleanZhuyin = zhuyin.trim()
        val cleanWord = word.trim()
        require(cleanZhuyin.isNotEmpty()) { "請輸入注音" }
        require(cleanWord.isNotEmpty()) { "請輸入詞彙" }

        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COL_ZHUYIN, cleanZhuyin)
            put(COL_WORD, cleanWord)
            put(COL_CREATED_AT, now)
            put(COL_UPDATED_AT, now)
        }
        return writableDatabase.insertWithOnConflict(
            TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun updateEntry(id: Long, zhuyin: String, word: String): Boolean {
        val cleanZhuyin = zhuyin.trim()
        val cleanWord = word.trim()
        require(cleanZhuyin.isNotEmpty()) { "請輸入注音" }
        require(cleanWord.isNotEmpty()) { "請輸入詞彙" }

        val values = ContentValues().apply {
            put(COL_ZHUYIN, cleanZhuyin)
            put(COL_WORD, cleanWord)
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }
        return writableDatabase.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString())) > 0
    }

    fun deleteEntry(id: Long): Boolean =
        writableDatabase.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString())) > 0

    fun search(query: String): List<UserDictionaryEntry> {
        val cleanQuery = query.trim()
        val selection: String?
        val args: Array<String>?
        if (cleanQuery.isEmpty()) {
            selection = null
            args = null
        } else {
            selection = "$COL_ZHUYIN LIKE ? OR $COL_WORD LIKE ?"
            args = arrayOf("%$cleanQuery%", "%$cleanQuery%")
        }
        return readEntries(selection, args, "$COL_UPDATED_AT DESC, $COL_ID DESC")
    }

    fun getCandidates(zhuyin: String): List<String> =
        readEntries("$COL_ZHUYIN = ?", arrayOf(zhuyin), "$COL_UPDATED_AT DESC, $COL_ID DESC")
            .map { it.word }

    fun getCandidates(zhuyinKeys: List<String>): List<String> {
        val merged = mutableListOf<String>()
        for (key in zhuyinKeys) {
            getCandidates(key).forEach { word ->
                if (word !in merged) merged.add(word)
            }
        }
        return merged
    }

    fun exportToJson(): String {
        val array = JSONArray()
        search("").forEach { entry ->
            array.put(
                JSONObject()
                    .put("zhuyin", entry.zhuyin)
                    .put("word", entry.word)
                    .put("createdAt", entry.createdAt)
                    .put("updatedAt", entry.updatedAt)
            )
        }
        return JSONObject()
            .put("version", 1)
            .put("entries", array)
            .toString(2)
    }

    fun exportToUri(resolver: ContentResolver, uri: Uri) {
        resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
            writer.write(exportToJson())
        } ?: error("無法寫入匯出檔案")
    }

    fun importFromUri(resolver: ContentResolver, uri: Uri): Int {
        val text = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("無法讀取匯入檔案")
        return importFromText(text)
    }

    fun importFromText(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            importJson(trimmed)
        } else {
            importTsv(trimmed)
        }
    }

    private fun importJson(text: String): Int {
        val entries = if (text.startsWith("[")) {
            JSONArray(text)
        } else {
            JSONObject(text).optJSONArray("entries") ?: JSONArray()
        }

        var count = 0
        writableDatabase.beginTransaction()
        try {
            for (i in 0 until entries.length()) {
                val obj = entries.optJSONObject(i) ?: continue
                val zhuyin = obj.optString("zhuyin").trim()
                val word = obj.optString("word").trim()
                if (zhuyin.isEmpty() || word.isEmpty()) continue
                addEntry(zhuyin, word)
                count++
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
        return count
    }

    private fun importTsv(text: String): Int {
        var count = 0
        writableDatabase.beginTransaction()
        try {
            text.lineSequence().forEach { line ->
                val cleanLine = line.trim()
                if (cleanLine.isEmpty() || cleanLine.startsWith("#")) return@forEach
                val parts = cleanLine.split('\t', ',', limit = 2)
                if (parts.size < 2) return@forEach
                addEntry(parts[0], parts[1])
                count++
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
        return count
    }

    private fun readEntries(
        selection: String?,
        selectionArgs: Array<String>?,
        orderBy: String
    ): List<UserDictionaryEntry> {
        val result = mutableListOf<UserDictionaryEntry>()
        readableDatabase.query(
            TABLE_NAME,
            arrayOf(COL_ID, COL_ZHUYIN, COL_WORD, COL_CREATED_AT, COL_UPDATED_AT),
            selection,
            selectionArgs,
            null,
            null,
            orderBy
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(COL_ID)
            val zhuyinIndex = cursor.getColumnIndexOrThrow(COL_ZHUYIN)
            val wordIndex = cursor.getColumnIndexOrThrow(COL_WORD)
            val createdIndex = cursor.getColumnIndexOrThrow(COL_CREATED_AT)
            val updatedIndex = cursor.getColumnIndexOrThrow(COL_UPDATED_AT)
            while (cursor.moveToNext()) {
                result.add(
                    UserDictionaryEntry(
                        id = cursor.getLong(idIndex),
                        zhuyin = cursor.getString(zhuyinIndex),
                        word = cursor.getString(wordIndex),
                        createdAt = cursor.getLong(createdIndex),
                        updatedAt = cursor.getLong(updatedIndex)
                    )
                )
            }
        }
        return result
    }

    companion object {
        private const val DB_NAME = "user_dictionary.db"
        private const val DB_VERSION = 1
        private const val TABLE_NAME = "user_dictionary"
        private const val COL_ID = "_id"
        private const val COL_ZHUYIN = "zhuyin"
        private const val COL_WORD = "word"
        private const val COL_CREATED_AT = "created_at"
        private const val COL_UPDATED_AT = "updated_at"
    }
}
